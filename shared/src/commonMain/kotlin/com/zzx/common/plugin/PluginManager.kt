package com.zzx.common.plugin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.google.gson.Gson
import com.zzx.common.socket.model.Message
import com.zzx.common.socket.model.PluginUninstallEvent
import com.zzx.common.socket.type.CodeEnum

/**
 * 共享插件管理器
 */
object PluginManager {
    private val _plugins = mutableListOf<IPlugin>()
    private val _pluginFlow = MutableStateFlow<List<IPlugin>>(emptyList())
    val pluginFlow = _pluginFlow.asStateFlow()

    private val _transferFlow = MutableStateFlow<Map<String, Float>>(emptyMap())
    val transferFlow = _transferFlow.asStateFlow()

    private val _statusFlow = MutableStateFlow<String?>(null)
    val statusFlow = _statusFlow.asStateFlow()

    // 全局消息发送钩子 (由宿主注入)
    var globalMessageSender: ((String) -> Unit)? = null
    
    private val gson = Gson()

    // [核心改进] 全局任务注册表：确保每个插件 ID 在整个应用生命周期内只有一个活跃任务
    private val activeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // [核心改进] 单调实例 ID 追踪：跨 ClassLoader 识别“僵尸”消息的关键
    private val latestInstanceIds = mutableMapOf<String, Long>()

    // [核心改进] 插件源文件路径映射：解耦接口，防止二进制不兼容 (AbstractMethodError)
    private val pluginSourcePaths = mutableMapOf<String, String>()

    // [核心改进] 追踪 ClassLoader 以便卸载时释放文件锁 (Desktop 专用)
    private val pluginClassLoaders = mutableMapOf<String, Any>()

    /**
     * 校验传入的消息是否来自该插件的最新实例
     * @return 0: 丢弃(旧), 1: 接受(同代), 2: 接受并触发重置(全新高代)
     */
    fun checkMessageVersion(pluginId: String, incomingId: Long): Int {
        val currentMax = latestInstanceIds[pluginId] ?: 0L
        return when {
            incomingId > currentMax -> {
                latestInstanceIds[pluginId] = incomingId
                2 // 全新高代
            }
            incomingId == currentMax && incomingId != 0L -> 1 // 同代
            else -> 0 // 丢弃(旧或无效)
        }
    }

    /**
     * [二进制兼容性支持] 旧版插件 (如 SystemMonitorPlugin) 还在调用此方法。
     * @return true 如果 checkMessageVersion 返回 1 或 2 (即接受消息)
     */
    fun shouldProcessMessage(pluginId: String, incomingId: Long): Boolean {
        return checkMessageVersion(pluginId, incomingId) >= 1
    }

    fun registerJob(pluginId: String, job: kotlinx.coroutines.Job) {
        activeJobs[pluginId]?.cancel()
        activeJobs[pluginId] = job
    }

    fun cancelJob(pluginId: String) {
        activeJobs.remove(pluginId)?.cancel()
    }

    val plugins: List<IPlugin> get() = _plugins

    fun updateStatus(status: String) {
        println("Status Update: $status")
        _statusFlow.value = status
    }

    fun updateProgress(fileName: String, progress: Float) {
        val current = _transferFlow.value.toMutableMap()
        current[fileName] = progress
        _transferFlow.value = current
    }

    fun clearProgress(fileName: String) {
        val current = _transferFlow.value.toMutableMap()
        current.remove(fileName)
        _transferFlow.value = current
    }

    /**
     * 宿主接收到 PLUGIN_CUSTOM 消息后，路由给特定插件
     */
    fun dispatchPluginMessage(pluginId: String, data: String) {
        _plugins.find { it.id == pluginId }?.onReceive(data)
    }

    /**
     * 徹底移除一個插件 (包括本地销毁和可选的通知远程卸载)
     * @param trueUninstall 是否是真正物理卸载 (如果是内部替换/更新，则设为 false 以保留路径注册)
     */
    fun removePlugin(pluginId: String, notifyRemote: Boolean = false, trueUninstall: Boolean = true) {
        val existing = _plugins.filter { it.id == pluginId }
        // [核心修正] 即使内存中没有该插件的实例(existing为空)，只要是彻底卸载且注册表里有该 ID 的文件路径，也必须执行物理清理。
        // 这确保了加载失败的 APK 依然能被增量同步逻辑发现并清除。
        if (existing.isNotEmpty() || (trueUninstall && pluginSourcePaths.containsKey(pluginId))) {
            if (notifyRemote) {
                try {
                    val uninstallEvent = PluginUninstallEvent(pluginId)
                    val message = Message(CodeEnum.PLUGIN_UNINSTALL.value, "卸载插件", uninstallEvent)
                    globalMessageSender?.invoke(gson.toJson(message))
                } catch (e: Exception) {
                    println("Error notifying remote uninstall: ${e.message}")
                }
            }

            // 1. 销毁实例并释放资源 (核心：必须在删除文件前关闭 ClassLoader)
            existing.forEach { oldPlugin ->
                try {
                    println("Plugin Lifecycle: Destroying instance of ${oldPlugin.id}")
                    oldPlugin.onDestroy()
                    
                    // 尝试释放 ClassLoader (仅 Desktop/JVM 环境有效)
                    pluginClassLoaders.remove(oldPlugin.id)?.let { loader ->
                        try {
                            if (loader is java.io.Closeable) {
                                println("Plugin Lifecycle: Closing ClassLoader for ${oldPlugin.id}")
                                loader.close()
                            }
                        } catch (e: Exception) {
                            println("Warning: Failed to close ClassLoader: ${e.message}")
                        }
                    }
                } catch (e: Throwable) {
                    println("Warning: Error destroying plugin ${oldPlugin.id}: ${e.message}")
                }
            }

            // 2. 核心改进：无论是否通知远程，只要是真正物理卸载且本地有路径注册，就尝试从磁盘彻底删除
            if (trueUninstall) {
                val path = pluginSourcePaths[pluginId]
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        println("Plugin Uninstallation: Attempting robust deletion of $path")
                        var deleted = false
                        
                        // [物理删除优化策略] 循环尝试重命名并删除，解决 Windows 文件锁问题
                        repeat(5) { attempt ->
                            try {
                                System.gc()
                                System.runFinalization()
                                Thread.sleep(10) // 给予系统各层级释放句柄的时间
                                
                                // 优先重命名，确保即删不掉，重启也不会被加载
                                val deletedFile = File(file.parent, ".deleted_${pluginId}_${System.currentTimeMillis()}")
                                if (file.renameTo(deletedFile)) {
                                    if (deletedFile.delete() || java.nio.file.Files.deleteIfExists(deletedFile.toPath())) {
                                        deleted = true
                                        println("Plugin Uninstallation: Physical file deleted SUCCESS on attempt ${attempt + 1}")
                                        pluginSourcePaths.remove(pluginId)
                                        return@repeat
                                    }
                                } else if (file.delete() || java.nio.file.Files.deleteIfExists(file.toPath())) {
                                    deleted = true
                                    println("Plugin Uninstallation: Physical file deleted SUCCESS (direct) on attempt ${attempt + 1}")
                                    pluginSourcePaths.remove(pluginId)
                                    return@repeat
                                }
                            } catch (e: Exception) { /* 忽略单次失败 */ }
                        }
                        
                        if (!deleted) {
                            println("Plugin Uninstallation: WARNING - File locked. Marking as ghost but clearing registry.")
                            pluginSourcePaths.remove(pluginId) // 即使失败也移除注册，防止僵尸索引
                        }
                    }
                } else {
                    // 即使没有物理文件路径，由于是 trueUninstall，也要尝试清理注册表项
                    pluginSourcePaths.remove(pluginId)
                }
            }
            
            // 3. 从内存列表移除并同步 UI
            _plugins.removeAll { it.id == pluginId }
            _pluginFlow.value = _plugins.toList()
            
            // 最终垃圾回收
            System.gc()
            System.runFinalization()
        }
    }

    /**
     * [双端对齐] 根据桌面端提供的“权威 ID 列表”对手机端进行清理。
     * 如果本地存在列表之外的持久化插件，则自动执行物理卸载。
     */
    fun reconcilePlugins(authorizedIds: List<String>) {
        println("Sync: Starting reconciliation with ${authorizedIds.size} authorized plugins.")
        
        // 找出本地有但桌面端没有的插件 (排除内置插件，如果可以通过 ID 或包名识别)
        // 目前策略：凡是持久化在 plugins 目录下的，都应受桌面端管控
        val toRemove = _plugins.filter { it.id !in authorizedIds }
        
        if (toRemove.isEmpty()) {
            println("Sync: No unauthorized plugins found. State is clean.")
            return
        }

        toRemove.forEach { plugin ->
            println("Sync: Removing unauthorized persistent plugin: ${plugin.id}")
            // 执行本地卸载逻辑 (不需要再通知远程，因为指令本身来自远程)
            removePlugin(plugin.id, notifyRemote = false)
        }
    }

    fun addPlugin(plugin: IPlugin) {
        // 如果插件已存在，先执行替换逻辑 (注意：此处 trueUninstall 设为 false 以保留物理文件和注册路径)
        if (_plugins.any { it.id == plugin.id }) {
            removePlugin(plugin.id, notifyRemote = false, trueUninstall = false)
        }
        
        // 注入发送能力给插件 (增加对旧插件的兼容性处理)
        try {
            plugin.messageSender = { id, content ->
                // 这里我们构造一个通用的插件消息 JSON 发送出去
                // 注意：外层需要定义一个通用的 PLUGIN_CUSTOM 协议格式
                // 这里暂由插件自行负责或由宿主逻辑包装
                // 建议：直接通过 globalMessageSender 发送，由插件自行拼装完整的 JSON
                globalMessageSender?.invoke(content)
            }
        } catch (e: NoSuchMethodError) {
            println("Warning: Plugin ${plugin.id} does not support messageSender property (Legacy).")
        } catch (e: AbstractMethodError) {
            println("Warning: Plugin ${plugin.id} inherits an abstract messageSender setter but has no implementation (Binary Mismatch).")
        }

        _plugins.add(plugin)
        // 核心修正：强制按插件 id 排序，确保各端顺序一致
        _plugins.sortBy { it.id }
        // 发射列表副本以触发 UI 更新
        _pluginFlow.value = _plugins.toList()
    }

    /**
     * 从指定目录加载所有插件文件 (.apk, .aar, .jar)
     */
    fun loadPluginsFromDir(dirPath: String, loader: PluginLoader) {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            println("Plugin directory does not exist: $dirPath")
            return
        }

        println("Scanning plugins in: ${dir.absolutePath}")
        val fileArray = dir.listFiles { _, name ->
            name.endsWith(".apk") || name.endsWith(".aar") || name.endsWith(".jar")
        } ?: return

        // 核心修正：对扫描到的文件列表按名称预排序，保证加载进入内存的顺序稳定性
        val files = fileArray.toList().sortedBy { it.name }

        files.forEach { file ->
            println("Found persistent plugin file: ${file.name}")
            try {
                val loadedPlugin = loader.loadPluginAuto(file.absolutePath)
                if (loadedPlugin != null) {
                    println("Successfully loaded persistent plugin: ${loadedPlugin.name}")
                    addPlugin(loadedPlugin)
                } else {
                    println("Failed to load persistent plugin: ${file.name}")
                }
            } catch (e: Exception) {
                println("Error loading persistent plugin ${file.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 注册插件对应的源文件路径
     */
    fun registerSourcePath(pluginId: String, path: String) {
        pluginSourcePaths[pluginId] = path
    }

    /**
     * 注册插件对应的 ClassLoader (用于卸载时释放文件锁)
     */
    fun registerClassLoader(pluginId: String, loader: Any) {
        pluginClassLoaders[pluginId] = loader
    }

    /**
     * 获取注册的源文件路径 (用于增量同步)
     */
    fun getPluginSourcePath(pluginId: String): String? {
        return pluginSourcePaths[pluginId]
    }
}
