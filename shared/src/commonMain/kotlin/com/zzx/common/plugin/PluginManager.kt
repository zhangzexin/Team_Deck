package com.zzx.common.plugin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 共享插件管理器
 */
object PluginManager {
    private val _plugins = mutableListOf<IPlugin>()
    private val _pluginFlow = MutableStateFlow<List<IPlugin>>(emptyList())
    val pluginFlow = _pluginFlow.asStateFlow()

    private val _transferFlow = MutableStateFlow<Map<String, Float>>(emptyMap())
    val transferFlow = _transferFlow.asStateFlow()

    val plugins: List<IPlugin> get() = _plugins

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

    fun addPlugin(plugin: IPlugin) {
        if (_plugins.any { it.id == plugin.id }) {
            _plugins.removeAll { it.id == plugin.id }
        }
        _plugins.add(plugin)
        // 发射列表副本以触发 UI 更新
        _pluginFlow.value = _plugins.toList()
    }
}
