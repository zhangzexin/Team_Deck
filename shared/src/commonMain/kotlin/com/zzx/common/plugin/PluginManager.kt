package com.zzx.common.plugin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

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

    fun addPlugin(plugin: IPlugin) {
        if (_plugins.any { it.id == plugin.id }) {
            _plugins.removeAll { it.id == plugin.id }
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
