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
            val urls = mutableListOf<java.net.URL>()
            urls.add(file.toURI().toURL())

            // 核心增强：探测并提取 APK 内部嵌套的桌面端专用 JAR (Containerized Deps)
            try {
                java.util.zip.ZipFile(file).use { zip ->
                    val entry = zip.getEntry("assets/desktop-launcher.jar")
                    if (entry != null) {
                        val tempJar = File.createTempFile("desktop_launcher_", ".jar")
                        tempJar.deleteOnExit()
                        zip.getInputStream(entry).use { input ->
                            tempJar.outputStream().use { output -> input.copyTo(output) }
                        }
                        urls.add(tempJar.toURI().toURL())
                        println("Containerized JAR detected and injected: ${tempJar.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                // 如果没有嵌套 JAR，说明是简单插件，继续使用基础类路径
            }

            val classLoader = java.net.URLClassLoader(urls.toTypedArray(), this.javaClass.classLoader)
            val pluginClass = classLoader.loadClass(mainClass)
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as IPlugin
            plugin
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
            
            // 从包内读取属性文件 (先后探测根目录与 assets 目录)
            val properties = java.util.Properties()
            var propStream = classLoader.getResourceAsStream("plugin.properties")
            if (propStream == null) {
                propStream = classLoader.getResourceAsStream("assets/plugin.properties")
            }
            
            if (propStream != null) {
                propStream.use { properties.load(it) }
                val mainClass = properties.getProperty("plugin.mainClass")
                if (!mainClass.isNullOrBlank()) {
                    println("Auto-discovered main class: $mainClass")
                    return loadPlugin(pluginPath, mainClass)
                }
            }
            
            // 属性文件读取失败，则直接视为无效插件，不再进行盲扫猜测
            println("No valid plugin metadata found in $pluginPath. Skipping...")
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
