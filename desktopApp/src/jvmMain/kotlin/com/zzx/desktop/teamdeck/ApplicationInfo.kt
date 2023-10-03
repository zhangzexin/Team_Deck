package com.zzx.desktop.teamdeck

import java.nio.file.Paths

/**
 *@描述：全局常用参数
 *@time：2023/10/2
 *@author:zhangzexin
 */
object ApplicationInfo {
    val packageName = "TeamDeck"
    val LocalAppData by lazy { Paths.get(System.getProperty("user.home"), "AppData", "Local",packageName) }
    val AppConfig by lazy { Paths.get(System.getProperty("user.home"), "AppData", "Roaming",packageName) }
}