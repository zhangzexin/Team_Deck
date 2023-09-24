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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    LaunchedEffect(Unit) {
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
        items(number) {
            Card(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .size(baseButton, baseButton)
                .clickable {
                    Toast
                        .makeText(context, "点击了$it", Toast.LENGTH_SHORT)
                        .show()
                }) {

            }
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