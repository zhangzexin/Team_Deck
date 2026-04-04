package com.zzx.desktop.teamdeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.zzx.desktop.teamdeck.utils.NsdManagerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import com.zzx.common.plugin.PluginManager
import com.zzx.common.plugin.PluginLoader

@OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PluginsLoadWidget() {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    var isDragging by remember { mutableStateOf(false) }
    val rememberCoroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxSize(0.5f)
            .background(if (!isDragging) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.inverseOnSurface, shape = RoundedCornerShape(10f))
            .drawBehind {
                drawRoundRect(
                    topLeft = Offset(10f, 10f),
                    size = Size(drawContext.size.width - 20f, drawContext.size.height - 20f),
                    color = Color.Black,
                    style = stroke,
                    cornerRadius = CornerRadius(10f,10f)
                )
            }.onExternalDrag(
            onDragStart = {
                isDragging = true
            },
            onDragExit = {
                isDragging = false
            },
            onDrag = {

            },
            onDrop = { state ->
                val dragData = state.dragData
//                text = dragData.toString()
//                println(dragData.toString())
                if (dragData is DragData.Image) {
//                    println(dragData.readImage().toString())
                } else if (dragData is DragData.FilesList) {
                    val files = dragData.readFiles()
                    files.forEach { fileUri ->
                        // 稳健解析本地路径：兼容 Windows 的 file:/// 协议
                        val path = try {
                            java.io.File(java.net.URI(fileUri)).absolutePath
                        } catch (e: Exception) {
                            fileUri.substringAfter("file:/").replace("%20", " ")
                        }
                        
                        println("Targeting Plugin Path: $path")
                        
                        // 仅处理 jar 或 apk
                        if (path.endsWith(".jar", ignoreCase = true) || path.endsWith(".apk", ignoreCase = true)) {
                            // 1. 智能加载：尝试已知插件类名
                            val candidates = listOf("com.zzx.plugin.ImagePlugin", "com.zzx.plugin.SamplePlugin")
                            var loadedPlugin: com.zzx.common.plugin.IPlugin? = null
                            
                            for (className in candidates) {
                                try {
                                    val plugin = PluginLoader().loadPlugin(path, className)
                                    if (plugin != null) {
                                        loadedPlugin = plugin
                                        println("Successfully loaded plugin class: $className")
                                        break
                                    }
                                } catch (e: Exception) {
                                    // 继续尝试下一个
                                }
                            }

                            if (loadedPlugin != null) {
                                PluginManager.addPlugin(loadedPlugin)
                            } else {
                                println("Failed to find any matching IPlugin implementation in: $path")
                            }
                            
                            // 2. 远程发送：将插件推送至手机端
                            rememberCoroutineScope.launch(Dispatchers.IO) {
                                try {
                                    NsdManagerUtils.Instance.sendFile(path)
                                    println("Universal Plugin sent to mobile: $path")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                isDragging = false
            }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource("system_update_alt-24px.xml"),
            contentDescription = "上传插件",
            modifier = Modifier.fillMaxSize(1f)
        )
    }
}