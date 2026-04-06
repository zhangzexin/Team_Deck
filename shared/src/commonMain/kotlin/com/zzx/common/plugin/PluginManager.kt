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
     * 彻底移除一个插件 (包括本地销毁和可选的通知远程卸载)
     */
    fun removePlugin(pluginId: String, notifyRemote: Boolean = false) {
        val existing = _plugins.filter { it.id == pluginId }
        if (existing.isNotEmpty()) {
            if (notifyRemote) {
                try {
                    val uninstallEvent = PluginUninstallEvent(pluginId)
                    val message = Message(CodeEnum.PLUGIN_UNINSTALL.value, "卸载插件", uninstallEvent)
                    globalMessageSender?.invoke(gson.toJson(message))
                } catch (e: Exception) {
                    println("Error notifying remote uninstall: ${e.message}")
                }
            }

            existing.forEach { oldPlugin ->
                try {
                    println("Plugin Lifecycle: Destroying instance of ${oldPlugin.id}")
                    oldPlugin.onDestroy()
                } catch (e: Throwable) {
                    println("Warning: Error destroying plugin ${oldPlugin.id}: ${e.message}")
                }
            }
            _plugins.removeAll { it.id == pluginId }
            _pluginFlow.value = _plugins.toList()
            
            // 建议回收资源
            System.gc()
            System.runFinalization()
        }
    }

    fun addPlugin(plugin: IPlugin) {
        // 如果插件已存在，先执行标准的本地移除流程 (不重复触发远程通知)
        if (_plugins.any { it.id == plugin.id }) {
            removePlugin(plugin.id, notifyRemote = false)
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
}
