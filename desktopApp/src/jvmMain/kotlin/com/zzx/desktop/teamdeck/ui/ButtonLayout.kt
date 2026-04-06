package com.zzx.desktop.teamdeck.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.zzx.common.plugin.IPlugin
import com.zzx.common.plugin.PluginManager
import com.zzx.desktop.teamdeck.md_theme_dark_outlineVariant
import com.zzx.desktop.teamdeck.ui.manager.LocalPluginDragState

@Composable
fun ButtonLayout(onPluginClick: (IPlugin) -> Unit) {
    var number by remember {
        mutableStateOf(6)
    }
    var fix_w by remember {
        mutableStateOf(2)
    }
    Box(modifier = Modifier.size(1100.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = this@BoxWithConstraints.maxWidth - 20.dp
            val height = this@BoxWithConstraints.maxHeight - 20.dp
            val fix_h = number / fix_w
            val baseButton = 100.dp
            val h = (height / baseButton).toInt()
            val w = (width / baseButton).toInt()
            val sw = (width - (baseButton * w)) / w
            val sh = (height - (baseButton * h)) / h
            ItemBuild(h * w, w, sw, sh, baseButton, onPluginClick)
        }
    }
}

@Composable
private fun ItemBuild(n: Int, w: Int, sw: Dp, sh: Dp, baseButton: Dp, onPluginClick: (IPlugin) -> Unit) {
    val pluginsState = PluginManager.pluginFlow.collectAsState()
    val loadedPlugins = pluginsState.value
    val dragState = LocalPluginDragState.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(w),
        horizontalArrangement = Arrangement.spacedBy(sw + 1.dp),
        verticalArrangement = Arrangement.spacedBy(sh + 1.dp),
        contentPadding = PaddingValues(vertical = sh, horizontal = sw),
        userScrollEnabled = false
    ) {
        // 1. 渲染已加载的插件
        items(loadedPlugins.size) { index ->
            val plugin = loadedPlugins[index]
            val isBeingDragged = dragState.isDragging && dragState.draggedPlugin?.id == plugin.id
            
            var itemOffset by remember { mutableStateOf(Offset.Zero) }
            Card(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .size(baseButton, baseButton)
                    .onGloballyPositioned { layoutCoordinates ->
                        itemOffset = layoutCoordinates.positionInWindow()
                    }
                    .pointerInput(plugin.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { pointerOffset ->
                                dragState.startDrag(plugin, itemOffset) 
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragState.updateOffset(dragAmount)
                            },
                            onDragEnd = {
                                dragState.endDrag()
                            },
                            onDragCancel = {
                                dragState.endDrag()
                            }
                        )
                    }
                    .clickable {
                        onPluginClick(plugin)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isBeingDragged) Color.Gray.copy(alpha = 0.5f) else Color.Transparent
                )
            ) {
                // 在网格中依然使用 DesktopUI 渲染卡片缩略图
                Box(modifier = Modifier.alpha(if (isBeingDragged) 0.3f else 1f)) {
                    plugin.DesktopUI()
                }
            }
        }
        
        // 2. 渲染剩余占位符
        val placeholderCount = (n - loadedPlugins.size).coerceAtLeast(0)
        items(placeholderCount) {
            Card(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).size(baseButton, baseButton)
                    .clickable {
                        // 占位符逻辑
                    },
                colors = CardDefaults.cardColors(containerColor = md_theme_dark_outlineVariant)
            ) {

            }
        }
    }
}