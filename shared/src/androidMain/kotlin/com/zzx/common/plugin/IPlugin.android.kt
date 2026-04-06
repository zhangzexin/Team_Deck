package com.zzx.common.plugin

/**
 * Android 端插件设置页面支持检查 (反射实现)
 */
actual fun IPlugin.isSettingsSupported(): Boolean {
    return try {
        // Compose 编译后的方法名带有参数 (Composer, Int)
        // 我们通过查找方法名并排除并接口默认实现来确定
        val clazz = this::class.java
        val hasMethod = clazz.declaredMethods.any { it.name.contains("SettingsUI") }
        val hasSuperMethod = if (!hasMethod) {
             // 递归检查父类 (不含接口)
             var superClass = clazz.superclass
             var found = false
             while (superClass != null && superClass != Object::class.java) {
                 if (superClass.declaredMethods.any { it.name.contains("SettingsUI") }) {
                     found = true
                     break
                 }
                 superClass = superClass.superclass
             }
             found
        } else true
        
        hasMethod || hasSuperMethod
    } catch (e: Throwable) {
        false
    }
}
