package com.zzx.common.plugin

/**
 * Desktop 端插件设置页面支持检查 (反射实现 / JVM)
 */
actual fun IPlugin.isSettingsSupported(): Boolean {
    return try {
        // Compose 编译后的方法名带有参数 (Composer, Int)
        // 我们检查当前类或其非接口父类中是否重写了 SettingsUI
        val clazz = this::class.java
        
        // 查找是否直接重写
        val hasMethod = clazz.declaredMethods.any { it.name.contains("SettingsUI") }
        
        // 向上查找 (排除 IPlugin 接口本身)
        var current: Class<*>? = clazz
        var found = false
        while (current != null && current != Object::class.java) {
             if (current.declaredMethods.any { it.name.contains("SettingsUI") }) {
                 found = true
                 break
             }
             current = current.superclass
        }
        
        found
    } catch (e: Throwable) {
        false
    }
}
