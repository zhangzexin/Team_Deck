package com.zzx.plugin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zzx.common.plugin.IPlugin

/**
 * 示例插件实现
 */
class SamplePlugin : IPlugin {
    override val id: String = "sample_plugin_01"
    override val name: String = "示例开关"

    @Composable
    override fun AppUI() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ON",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    @Composable
    override fun DesktopUI() {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("控制面板: 示例开关")
            Button(onClick = { onTrigger("click") }) {
                Text("触发动作")
            }
        }
    }

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        println("Plugin Triggered: $actionId with $params")
        // 这里可以执行具体业务逻辑，比如调用系统命令
    }
}
