package com.zzx.desktop.teamdeck.ui.root

import androidx.compose.runtime.Composable
import com.zzx.desktop.teamdeck.ui.screen.MainScreen
import com.zzx.desktop.teamdeck.ui.screen.PluginsScreen

@Composable
fun RootContent(pageIndex: () -> Int) {
    when(pageIndex()) {
        0 -> MainScreen()
        1 -> PluginsScreen()
    }
}