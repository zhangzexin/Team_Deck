package com.zzx.common.plugin

/**
 * 跨平台插件加载接口
 */
expect class PluginLoader() {
    /**
     * 核心加载逻辑，平台各自实现
     */
    fun loadPlugin(pluginPath: String, mainClass: String): IPlugin?
}
