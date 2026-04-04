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
            val url = file.toURI().toURL()
            val classLoader = URLClassLoader(arrayOf(url), this.javaClass.classLoader)
            val pluginClass = classLoader.loadClass(mainClass)
            pluginClass.getDeclaredConstructor().newInstance() as? IPlugin
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
