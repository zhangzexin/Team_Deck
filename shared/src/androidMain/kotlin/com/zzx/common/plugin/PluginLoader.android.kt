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
            
            // 属性文件读取失败，则直接视为无效插件，不再进行盲扫猜测
            Log.e("TeamDeck", "No valid plugin metadata found in $pluginPath. Skipping...")
            null
        } catch (e: Exception) {
            Log.e("TeamDeck", "Auto-load failed (Android): ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
