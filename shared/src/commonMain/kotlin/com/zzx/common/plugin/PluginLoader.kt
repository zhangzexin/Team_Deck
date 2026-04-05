package com.zzx.common.plugin

/**
 * 跨平台插件加载接口
 */
expect class PluginLoader() {
    /**
     * 指定启动类加载插件
     */
    fun loadPlugin(pluginPath: String, mainClass: String): IPlugin?

    /**
     * 自动从 plugin.properties 探测并加载插件
     */
    fun loadPluginAuto(pluginPath: String): IPlugin?
}
