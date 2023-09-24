package com.zzx.desktop.teamdeck.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zzx.common.socket.model.InitUiEvent
import com.zzx.common.socket.model.Message
import com.zzx.common.socket.type.CodeEnum
import com.zzx.desktop.teamdeck.socket.MdnsUtils
import com.zzx.desktop.teamdeck.flowBus.FlowBus
import com.zzx.desktop.teamdeck.ui.root.RootContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.zzx.desktop.teamdeck.utils.NsdManagerUtils

@Composable
@Preview
fun App1() {
    var index by remember { mutableStateOf(1) }
    var text by remember { mutableStateOf("start") }
    var number by remember {
        mutableStateOf(6)
    }
    var fix_w by remember {
        mutableStateOf(2)
    }
    val items = listOf("Home" to Icons.Outlined.Home, "Plugins" to Icons.Outlined.Favorite)
    var selectedItem by remember { mutableStateOf(items[0]) }
    val rememberCoroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        FlowBus.with<Message<InitUiEvent>>(CodeEnum.INITUI.name).register(this) {
            number = it.data.number
            fix_w = it.data.columns
            index = 9999
        }
    }
    LaunchedEffect(true) {
        rememberCoroutineScope.launch {
            withContext(Dispatchers.IO) {
                var port = 0
                NsdManagerUtils.Instance.mockWebSocket(this).let {
                    port = it.port
                    println("port:$port")
                }
                if (port > 0) {
                    MdnsUtils.instance.setUp(port)
                }
                while (true) {
                    delay(1000)
                    index += 1
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            MdnsUtils.instance.stop()
            FlowBus.with<Message<InitUiEvent>>(CodeEnum.INITUI.name).unRegister()
        }
    }

    Surface() {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            MainModalDrawerSheet(
                items = items,
                selectedItem = selectedItem,
                onItemsClick = { item ->
                    selectedItem = item
                }
            )
            RootContent{
                when(selectedItem.first) {
                    "Home" -> 0
                    "Plugins" -> 1
                    else -> {0}
                }
            }
        }
    }
}

