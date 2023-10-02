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
                    dragData.readFiles().first().let {
                        rememberCoroutineScope.launch(Dispatchers.IO) {
//                            NsdManagerUtils.Instance.sendFile(it)
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