package com.zzx.desktop.teamdeck.ui.manager

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.zzx.common.plugin.IPlugin

/**
 * 插件拖拽状态管理器
 */
class PluginDragState {
    var isDragging by mutableStateOf(false)
    var draggedPlugin by mutableStateOf<IPlugin?>(null)
    
    // 初始位置
    var initialPosition by mutableStateOf(Offset.Zero)
    // 累计位移
    var dragOffset by mutableStateOf(Offset.Zero)
    
    // 当前绝对位置 (用于碰撞检测和浮写)
    val currentPosition: Offset
        get() = initialPosition + dragOffset
    
    // 垃圾桶的全局位置
    var trashRect by mutableStateOf<Rect?>(null)
    
    // 是否悬停在垃圾桶上方
    val isHoveringTrash: Boolean
        get() = trashRect?.contains(currentPosition) ?: false

    fun startDrag(plugin: IPlugin, startPosition: Offset) {
        draggedPlugin = plugin
        initialPosition = startPosition
        dragOffset = Offset.Zero
        isDragging = true
    }

    fun updateOffset(delta: Offset) {
        dragOffset += delta
    }

    fun endDrag() {
        isDragging = false
    }
    
    fun clear() {
        draggedPlugin = null
        initialPosition = Offset.Zero
        dragOffset = Offset.Zero
    }
}

val LocalPluginDragState = staticCompositionLocalOf { PluginDragState() }
