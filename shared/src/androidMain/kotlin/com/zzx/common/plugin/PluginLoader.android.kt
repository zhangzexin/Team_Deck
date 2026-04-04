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
        val optimizedDir = currentContext.getCodeCacheDir().absolutePath
        
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
}
