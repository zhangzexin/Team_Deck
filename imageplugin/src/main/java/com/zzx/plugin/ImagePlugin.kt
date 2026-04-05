package com.zzx.plugin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.zzx.common.plugin.IPlugin
import com.zzx.common.util.ImageDecoder

/**
 * 独立 ImagePlugin 模块示例
 */
class ImagePlugin : IPlugin {
    override val id: String = "plugin_image_module_demo"
    override val name: String = "Image Module"
    override var messageSender: ((pluginId: String, data: String) -> Unit)? = null

    @Composable
    override fun AppUI() {
        val imageBitmap = remember { loadPluginIcon() }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Plugin Icon",
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("🖼️", style = MaterialTheme.typography.headlineMedium)
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    @Composable
    override fun DesktopUI() {
        AppUI()
    }

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        println("ImagePlugin (Standalone) triggered: $actionId")
    }

    private fun loadPluginIcon(): ImageBitmap? {
        return try {
            val stream = this.javaClass.classLoader.getResourceAsStream("res/drawable/plugin_image.png")
            if (stream != null) {
                ImageDecoder.decode(stream.readBytes())
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
