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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PluginsLoadWidget() {
    val isConnected by NsdManagerUtils.Instance.isConnected.collectAsState()
    val connectionType by NsdManagerUtils.Instance.connectionType.collectAsState()

    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    var isDragging by remember { mutableStateOf(false) }
    val rememberCoroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 顶部连接状态栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected) "已连接: $connectionType" else "未连接 (正在监听...)",
                style = MaterialTheme.typography.labelLarge,
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    if (!isDragging) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), 
                    shape = RoundedCornerShape(12.dp)
                )
                .drawBehind {
                    drawRoundRect(
                        color = if (isDragging) Color.Blue else Color.Gray,
                        style = stroke,
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
                .onExternalDrag(
                    onDragStart = { isDragging = true },
                    onDragExit = { isDragging = false },
                    onDrop = { state ->
                        val dragData = state.dragData
                        if (dragData is DragData.FilesList) {
                            val files = dragData.readFiles()
                            files.forEach { fileUri ->
                                val path = try {
                                    java.io.File(java.net.URI(fileUri)).absolutePath
                                } catch (e: Exception) {
                                    fileUri.substringAfter("file:/").replace("%20", " ")
                                }
                                
                                if (path.endsWith(".jar", ignoreCase = true) || path.endsWith(".apk", ignoreCase = true)) {
                                    try {
                                        println("Loading plugin directly from: $path")
                                        val loadedPlugin = PluginLoader().loadPluginAuto(path)
                                        if (loadedPlugin != null) {
                                            PluginManager.addPlugin(loadedPlugin)
                                            rememberCoroutineScope.launch(Dispatchers.IO) {
                                                // 远程发送：将插件推送至手机端
                                                NsdManagerUtils.Instance.sendFile(path)
                                            }
                                        } else {
                                            println("Failed to auto-load plugin from: $path")
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        isDragging = false
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource("system_update_alt-24px.xml"),
                    contentDescription = "拖入插件",
                    modifier = Modifier.size(48.dp),
                    tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (isDragging) "松开以安装插件" else "将插件 APK 拖入此处",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
