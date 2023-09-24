package com.zzx.desktop.teamdeck.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import javax.swing.JPanel
import kotlin.math.roundToInt

//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun DropBoxPanel(
//    window: ComposeWindow,
//    modifier: Modifier = Modifier,
//    component: JPanel = JPanel(),
//    onFileDrop: (List<String>) -> Unit
//) {
//
//    val dropBoundsBean = remember {
//        mutableStateOf(DropBoundsBean())
//    }
//
//    Box(
//        modifier = modifier.onPlaced {
//            dropBoundsBean.value = DropBoundsBean(
//                x = it.positionInWindow().x,
//                y = it.positionInWindow().y,
//                width = it.size.width,
//                height = it.size.height
//            )
//        }) {
//        LaunchedEffect(true) {
//            component.setBounds(
//                dropBoundsBean.value.x.roundToInt(),
//                dropBoundsBean.value.y.roundToInt(),
//                dropBoundsBean.value.width,
//                dropBoundsBean.value.height
//            )
//            window.contentPane.add(component)
//
//            val target = object : DropTarget(null, object : DropTargetAdapter() {
//                override fun drop(event: DropTargetDropEvent) {
//
//                    event.acceptDrop(DnDConstants.ACTION_REFERENCE)
//                    val dataFlavors = event.transferable.transferDataFlavors
//                    dataFlavors.forEach {
//                        if (it == DataFlavor.javaFileListFlavor) {
//                            val list = event.transferable.getTransferData(it) as List<*>
//
//                            val pathList = mutableListOf<String>()
//                            list.forEach { filePath ->
//                                pathList.add(filePath.toString())
//                            }
//                            onFileDrop(pathList)
//                        }
//                    }
//                    event.dropComplete(true)
//
//                }
//            }
//            ) {
//
//            }
//            window.contentPane.getComponent(1).dropTarget = target
//        }
//
//        SideEffect {
//            component.setBounds(
//                dropBoundsBean.value.x.roundToInt(),
//                dropBoundsBean.value.y.roundToInt(),
//                dropBoundsBean.value.width,
//                dropBoundsBean.value.height
//            )
//        }
//
//        DisposableEffect(true) {
//            onDispose {
//                window.contentPane.remove(component)
//            }
//        }
//    }
//}
//
data class DropBoundsBean(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val width: Int = 0,
    val height: Int = 0
)

@Composable
fun DropBoxPanel(
    modifier: Modifier,
    window: ComposeWindow,
    component: JPanel = JPanel(),
    onFileDrop: (String) -> Unit
) {

    val dropBoundsBean = remember {
        mutableStateOf(DropBoundsBean())
    }

    Box(
        modifier = modifier.onPlaced {
            dropBoundsBean.value = DropBoundsBean(
                x = it.positionInWindow().x,
                y = it.positionInWindow().y,
                width = it.size.width,
                height = it.size.height
            )
        }) {
        LaunchedEffect(true) {
            component.setBounds(
                dropBoundsBean.value.x.roundToInt(),
                dropBoundsBean.value.y.roundToInt(),
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
//            window.contentPane.add(component)

            //https://dev.to/tkuenneth/from-swing-to-jetpack-compose-desktop-2-4a4h
            val target = object : DropTarget() {
                @Synchronized
                override fun drop(evt: DropTargetDropEvent) {
                    evt.acceptDrop(DnDConstants.ACTION_REFERENCE)
                    val droppedFiles = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    droppedFiles.first()?.let {
                        onFileDrop(it.toString())
                    }
                }
            }

            window.contentPane.components[0].dropTarget = target
        }

        SideEffect {
            component.setBounds(
                dropBoundsBean.value.x.roundToInt(),
                dropBoundsBean.value.y.roundToInt(),
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
        }

        DisposableEffect(true) {
            onDispose {
                window.contentPane.remove(component)
            }
        }
    }
}

