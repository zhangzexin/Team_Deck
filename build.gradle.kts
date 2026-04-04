plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("multiplatform").apply(false)
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("org.jetbrains.compose").apply(false)
    id("org.jetbrains.kotlin.android").version("1.9.0").apply(false)
}

tasks.register("runAll") {
    group = "application"
    description = "同时启动 Android 和 Desktop 端应用"
    
    // 依赖于 Android 的安装任务
    dependsOn(":androidApp:installDebug")
    
    doLast {
        println("🚀 正在通过 ADB 唤起手机端 MainActivity...")
        try {
            exec {
                commandLine("adb", "shell", "am", "start", "-n", "com.zzx.android.teamdeck/com.zzx.android.teamdeck.MainActivity")
            }
        } catch (e: Exception) {
            println("⚠️ ADB 唤起失败，请确保手机已连接并开启开发者模式: ${e.message}")
        }
    }
    
    // 最终触发桌面端运行
    finalizedBy(":desktopApp:run")
}
