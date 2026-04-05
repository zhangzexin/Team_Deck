package com.zzx.desktop.teamdeck.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zzx.common.plugin.IPlugin
import com.zzx.desktop.teamdeck.ui.ButtonLayout

@Composable
fun MainScreen(onPluginClick: (IPlugin) -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        ButtonLayout(onPluginClick = onPluginClick)
    }
}