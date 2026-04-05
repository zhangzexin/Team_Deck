package com.zzx.android.teamdeck.ui.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.zzx.android.teamdeck.baseMaxHeight
import com.zzx.android.teamdeck.baseMaxWidth
import com.zzx.common.socket.model.InitEvent
import com.zzx.common.socket.model.InitUiEvent
import com.zzx.common.socket.model.Message
import com.zzx.common.socket.type.CodeEnum
import com.zzx.common.socket.type.InitMessageType
import com.zzx.android.teamdeck.drawer.CustomizeNavigationDrawer
import com.zzx.android.teamdeck.flowBus.FlowBus
import com.zzx.android.teamdeck.socket.MessageHandler
import com.zzx.android.teamdeck.ui.theme.AppTheme
import com.zzx.android.teamdeck.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 *@描述：
 *@time：2023/9/6
 *@author:zhangzexin
 */
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TeamDeckApp(viewModel: MainViewModel) {
    AppTheme {
        val collectAsState = viewModel.drawerValue.collectAsState()
        CustomizeNavigationDrawer(
            drawerState = collectAsState.value,
            modifier = Modifier.fillMaxWidth(),
            withsize = 200.dp,
            drawerContent = {
                AppDrawer(viewModel)

//                ModalDrawerSheet(Modifier.width(200.dp)) {
//                    Spacer(Modifier.height(12.dp))
//                    items.forEach { item ->
//                        NavigationDrawerItem(
//                            icon = { Icon(item, contentDescription = null) },
//                            label = { Text(item.name) },
//                            selected = item == selectedItem.value,
//                            onClick = {
//                                scope.launch {
//                                    drawerState.close()
//                                }
//                                selectedItem.value = item
//                            },
//                            modifier = Modifier.padding(horizontal = 12.dp)
//                        )
//                    }
//                }


            }) {
            BoxWithConstraints {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val orientation = LocalConfiguration.current.orientation
                    val baseButton = getBaseButton(maxWidth, orientation)
                    val width = maxWidth - 20.dp
                    val height = maxHeight - 20.dp
                    val h = (height / baseButton).toInt()
                    val w = (width / baseButton).toInt()
                    val sw = (width - (baseButton * w)) / w
                    val sh = (height - (baseButton * h)) / h
                    ItemBuild(h * w, w, sw, sh, baseButton, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ItemBuild(number: Int, w: Int, sw: Dp, sh: Dp, baseButton: Dp, viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 监听整个列表的变化
    val pluginsState = com.zzx.common.plugin.PluginManager.pluginFlow.collectAsState()
    val loadedPlugins = pluginsState.value

    // 监听正在传输的文件进度
    val transferState = com.zzx.common.plugin.PluginManager.transferFlow.collectAsState()
    val ongoingTransfers = transferState.value.toList()
    
    LaunchedEffect(Unit) {
        launch {
            viewModel.webSocketHandler.connectionSuccessEvent.collect { isWired ->
                val type = if (isWired) "有线 (USB/ADB)" else "无线 (WiFi)"
                Toast.makeText(context, "连接成功 ($type)", Toast.LENGTH_SHORT).show()
            }
        }

        // 【新增】启动自动连接：尝试有线 (USB/ADB) 模式
        viewModel.connect("127.0.0.1", 8888)
        
        FlowBus.with<Message<InitEvent>>(InitMessageType.code.name).register(lifecycleOwner) {
            val json = MessageHandler.gson.toJson(
                Message<InitUiEvent>(
                    CodeEnum.INITUI.value,
                    "",
                    InitUiEvent(number, w)
                )
            )
            viewModel.sendMessage(json)
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(w),
        horizontalArrangement = Arrangement.spacedBy(sw + 1.dp),
        verticalArrangement = Arrangement.spacedBy(sh + 1.dp),
        contentPadding = PaddingValues(vertical = sh, horizontal = sw),
        userScrollEnabled = false
    ) {
        // 1. 渲染加载完成的插件
        items(loadedPlugins.size) { index ->
            val plugin = loadedPlugins[index]
            Card(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .size(baseButton, baseButton)
                    .clickable {
                        plugin.onTrigger("click")
                    },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                plugin.AppUI()
            }
        }

        // 2. 渲染正在传输的进度格子
        items(ongoingTransfers.size) { index ->
            val (fileName, progress) = ongoingTransfers[index]
            ProgressItem(fileName, progress, baseButton)
        }
        
        // 3. 渲染剩余的占位符 (计算修正)
        val placeholderCount = (number - loadedPlugins.size - ongoingTransfers.size).coerceAtLeast(0)
        items(placeholderCount) {
            Card(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .size(baseButton, baseButton)
                .clickable {
                    Toast
                        .makeText(context, "点击了${it + loadedPlugins.size + ongoingTransfers.size}", Toast.LENGTH_SHORT)
                        .show()
                }) {

            }
        }
    }
}

@Composable
fun ProgressItem(fileName: String, progress: Float, size: Dp) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .size(size, size),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "正在接收...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getBaseButton(maxWidth: Dp, orientation: Int): Dp {
    val baseButtonSize = 100.dp
    return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        maxWidth / baseMaxWidth * baseButtonSize
    } else {
        maxWidth / baseMaxHeight * baseButtonSize
    }

}