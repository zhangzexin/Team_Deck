package com.zzx.desktop.teamdeck.ui.root

import androidx.compose.runtime.Composable
import com.zzx.common.plugin.IPlugin
import com.zzx.desktop.teamdeck.ui.screen.MainScreen
import com.zzx.desktop.teamdeck.ui.screen.PluginSettingsScreen
import com.zzx.desktop.teamdeck.ui.screen.PluginsScreen

@Composable
fun RootContent(
    activePlugin: IPlugin? = null,
    onBack: () -> Unit,
    onPluginClick: (IPlugin) -> Unit,
    pageIndex: () -> Int
) {
    if (activePlugin != null) {
        // [新架构] 如果激活了某个插件设置，则渲染对应的设置页
        PluginSettingsScreen(plugin = activePlugin, onBack = onBack)
    } else {
        // 原有导航逻辑
        when(pageIndex()) {
            0 -> MainScreen(onPluginClick = onPluginClick)
            1 -> PluginsScreen()
        }
    }
}