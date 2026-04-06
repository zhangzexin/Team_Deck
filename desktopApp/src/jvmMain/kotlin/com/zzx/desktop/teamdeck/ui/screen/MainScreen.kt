package com.zzx.desktop.teamdeck.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.zzx.common.plugin.IPlugin
import com.zzx.common.plugin.PluginManager
import com.zzx.desktop.teamdeck.ui.ButtonLayout
import com.zzx.desktop.teamdeck.ui.manager.LocalPluginDragState
import com.zzx.desktop.teamdeck.ui.manager.PluginDragState
import kotlin.math.roundToInt

@Composable
fun MainScreen(onPluginClick: (IPlugin) -> Unit) {
    val dragState = remember { PluginDragState() }
    var screenPosition by remember { mutableStateOf(Offset.Zero) }
    
    // [核心修正] 将卸载逻辑移至最外层，确保在拖拽结束（isDragging 变为 false）时，Effect 不会被销毁
    LaunchedEffect(dragState.isDragging) {
        if (!dragState.isDragging && dragState.draggedPlugin != null) {
            if (dragState.isHoveringTrash) {
                val pluginId = dragState.draggedPlugin!!.id
                println("Drop detected on Trash! Uninstalling $pluginId")
                com.zzx.common.plugin.PluginManager.removePlugin(pluginId, notifyRemote = true)
            }
            dragState.clear()
        }
    }

    CompositionLocalProvider(LocalPluginDragState provides dragState) {
        Box(modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenPosition = it.positionInWindow() }
        ) {
            // 1. 插件列表 (主层)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                ButtonLayout(onPluginClick = onPluginClick)
            }

            // 2. 垃圾桶 (悬浮层)
            if (dragState.isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (dragState.isHoveringTrash) Color.Red else Color.Black.copy(alpha = 0.4f))
                        .onGloballyPositioned { dragState.trashRect = it.boundsInWindow() }
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = null, 
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("拖拽至此卸载", color = Color.White, style = androidx.compose.material.MaterialTheme.typography.caption)
                    }
                }
            }

            // 3. 正在拖拽的影子 (最顶层)
            if (dragState.isDragging && dragState.draggedPlugin != null) {
                val plugin = dragState.draggedPlugin!!
                
                // 计算相对于 MainScreen 的偏移量 (纠正位置偏差)
                val relativeOffset = dragState.currentPosition - screenPosition
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(relativeOffset.x.roundToInt(), relativeOffset.y.roundToInt()) }
                        .size(100.dp)
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                        .zIndex(100f)
                ) {
                    plugin.DesktopUI()
                }
            }
        }
    }
}