package com.zzx.desktop.teamdeck.plugin

import java.net.URLClassLoader

/**
 *@描述：插件加载
 *@time：2023/10/2
 *@author:zhangzexin
 */
object PluginManager {

        var pluginClassLoader: ClassLoader? = null

        fun loadPlugin(pluginPath:String) {

//            val inputStream = context.assets.open("plugin.apk")
//            val filesDir = context.externalCacheDir
//            val apkFile = File(filesDir?.absolutePath, "plugin.apk")
//            apkFile.writeBytes(inputStream.readBytes())
//
//            val dexFile = File(filesDir, "dex")
//            if (!dexFile.exists()) dexFile.mkdirs()
//            println("dexPath: $dexFile")
            val jarResource = javaClass.classLoader.getResource(pluginPath)
            pluginClassLoader = URLClassLoader(arrayOf(jarResource))


//            classLoader.getResource()
//            pluginClassLoader = DexClassLoader(
//                apkFile.absolutePath,
//                dexFile.absolutePath,
//                null,
//                this.javaClass.classLoader
//            )
        }

        fun loadClass(className: String): Class<*>? {
            try {
                return pluginClassLoader?.loadClass(className)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
            return null
        }

}