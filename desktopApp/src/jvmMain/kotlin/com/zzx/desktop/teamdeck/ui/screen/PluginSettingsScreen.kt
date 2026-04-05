package com.zzx.desktop.teamdeck.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zzx.common.plugin.IPlugin

@Composable
fun PluginSettingsScreen(plugin: IPlugin, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        // 顶部导航栏 (宿主提供)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.Transparent)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colors.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${plugin.name} - 配置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
        }

        // 分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.1f))
        )

        // 中间内容区 (由插件定义的 SettingsUI 填充)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            plugin.SettingsUI()
        }
    }
}
