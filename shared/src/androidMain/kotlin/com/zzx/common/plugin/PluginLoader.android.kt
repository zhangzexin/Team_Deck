package com.zzx.common.plugin

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Android 平台插件加载实现
 */
actual class PluginLoader actual constructor() {
    private var context: Context? = null
    
    constructor(ctx: Context) : this() {
        this.context = ctx
    }
    
    /**
     * 加载指定路径的 APK 插件
     * @param pluginPath APK 文件全名
     * @param mainClass 插件实现类的全名
     */
    actual fun loadPlugin(pluginPath: String, mainClass: String): IPlugin? {
        val dexFile = File(pluginPath)
        if (!dexFile.exists()) return null
        
        val currentContext = context ?: return null
        val optimizedDir = currentContext.codeCacheDir.absolutePath
        
        return try {
            val classLoader = DexClassLoader(
                dexFile.absolutePath,
                optimizedDir,
                null,
                currentContext.classLoader
            )
            
            Log.d("TeamDeck", "Attempting to load class: $mainClass")
            val pluginClass = classLoader.loadClass(mainClass)
            val instance = pluginClass.getDeclaredConstructor().newInstance()
            
            Log.d("TeamDeck", "Instance created: ${instance.javaClass.name}")
            if (instance is IPlugin) {
                Log.d("TeamDeck", "Successfully cast to IPlugin")
                instance
            } else {
                Log.e("TeamDeck", "Cast failed! Instance implements: ${instance.javaClass.interfaces.joinToString { it.name }}")
                null
            }
        } catch (e: Exception) {
            Log.e("TeamDeck", "Load failed with exception: ${e.message}")
            Log.e("TeamDeck", "Stack trace: ${Log.getStackTraceString(e)}")
            e.printStackTrace()
            null
        }
    }

    /**
     * 自动从 plugin.properties 探测并加载插件 (Android 实现)
     */
    actual fun loadPluginAuto(pluginPath: String): IPlugin? {
        val dexFile = File(pluginPath)
        if (!dexFile.exists()) return null
        
        val currentContext = context ?: return null
        val optimizedDir = currentContext.codeCacheDir.absolutePath
        
        return try {
            val classLoader = DexClassLoader(
                dexFile.absolutePath,
                optimizedDir,
                null,
                currentContext.classLoader
            )
            
            // 安卓端专用：使用 ZipFile 手动从 APK 根目录读取属性文件 (ClassLoader.getResource 在此处不可靠)
            val properties = java.util.Properties()
            var zipFile: java.util.zip.ZipFile? = null
            try {
                zipFile = java.util.zip.ZipFile(dexFile)
                val entries = zipFile.entries()
                var found = false
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith("plugin.properties")) {
                        Log.d("TeamDeck", "Found metadata entry: ${entry.name}")
                        zipFile.getInputStream(entry).use { properties.load(it) }
                        val mainClass = properties.getProperty("plugin.mainClass")
                        if (!mainClass.isNullOrBlank()) {
                            Log.d("TeamDeck", "Auto-discovered main class (Android via Zip): $mainClass")
                            return loadPlugin(pluginPath, mainClass)
                        } else {
                            Log.e("TeamDeck", "plugin.properties found at ${entry.name} but plugin.mainClass is empty!")
                        }
                        found = true
                        break
                    }
                }
                
                if (!found) {
                    Log.e("TeamDeck", "plugin.properties NOT FOUND in ZIP. Trying hardcoded candidates...")
                }
            } finally {
                zipFile?.close()
            }
            
            // 兜底方案：尝试已知类名
            val candidates = listOf("com.zzx.plugin.ImagePlugin", "com.zzx.plugin.SamplePlugin")
            for (className in candidates) {
                try {
                    val plugin = loadPlugin(pluginPath, className)
                    if (plugin != null) {
                        Log.d("TeamDeck", "Android Fallback success! Loaded: $className")
                        return plugin
                    }
                } catch (e: Exception) {
                    // 继续尝试
                }
            }
            
            Log.e("TeamDeck", "Auto-discovery failed for $pluginPath")
            null
        } catch (e: Exception) {
            Log.e("TeamDeck", "Auto-load failed (Android): ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
