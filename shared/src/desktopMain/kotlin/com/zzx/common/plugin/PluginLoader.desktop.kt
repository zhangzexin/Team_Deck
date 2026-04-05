package com.zzx.common.plugin

import java.io.File
import java.net.URLClassLoader

/**
 * Desktop (JVM) 平台插件加载实现
 */
actual class PluginLoader actual constructor() {
    
    /**
     * 加载桌面端插件字节码 (支持 jar / zip 或打包类文件的 APK)
     */
    actual fun loadPlugin(pluginPath: String, mainClass: String): IPlugin? {
        val file = File(pluginPath)
        if (!file.exists()) return null
        
        return try {
            val url = File(pluginPath).toURI().toURL()
            val classLoader = java.net.URLClassLoader(arrayOf(url), this.javaClass.classLoader)
            val pluginClass = classLoader.loadClass(mainClass)
            pluginClass.getDeclaredConstructor().newInstance() as? IPlugin
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 自动从 plugin.properties 探测并加载插件 (桌面端实现)
     */
    actual fun loadPluginAuto(pluginPath: String): IPlugin? {
        val file = File(pluginPath)
        if (!file.exists()) return null
        
        return try {
            val url = File(pluginPath).toURI().toURL()
            val classLoader = java.net.URLClassLoader(arrayOf(url), this.javaClass.classLoader)
            
            // 从包内读取属性文件
            val properties = java.util.Properties()
            val propStream = classLoader.getResourceAsStream("plugin.properties")
            if (propStream != null) {
                propStream.use { properties.load(it) }
                val mainClass = properties.getProperty("plugin.mainClass")
                if (!mainClass.isNullOrBlank()) {
                    println("Auto-discovered main class: $mainClass")
                    return loadPlugin(pluginPath, mainClass)
                }
            }
            
            // 兜底方案：尝试已知类名
            println("Auto-discovery failed. Trying hardcoded candidates...")
            val candidates = listOf("com.zzx.plugin.ImagePlugin", "com.zzx.plugin.SamplePlugin")
            for (className in candidates) {
                try {
                    val plugin = loadPlugin(pluginPath, className)
                    if (plugin != null) {
                        println("Fallback success! Loaded: $className")
                        return plugin
                    }
                } catch (e: Exception) {
                    // 继续尝试
                }
            }
            
            println("No valid plugin found in $pluginPath")
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
