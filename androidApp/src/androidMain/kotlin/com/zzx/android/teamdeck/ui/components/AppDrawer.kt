package com.zzx.android.teamdeck.ui.components

import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zzx.android.teamdeck.InfoCallBack
import com.zzx.android.teamdeck.NsdManagerTool
import com.zzx.android.teamdeck.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *@描述：
 *@time：2023/9/6
 *@author:zhangzexin
 */
@Composable
fun AppDrawer(viewModel: MainViewModel) {
    val context = LocalContext.current
    val nsdManagerTool by lazy {
        NsdManagerTool(context)
    }
    var searchList = remember {
        mutableStateMapOf<String, NsdServiceInfo>().also {
            // test
//            it["12314315"] = NsdServiceInfo().also { nsd-> nsd.serviceName="test" }
        }
    }
    var isSearch = rememberSaveable {
        mutableStateOf(false)
    }
    val mConnectionInfo = remember { mutableStateOf<NsdServiceInfo?>(null) }
    LaunchedEffect(nsdManagerTool) {
        nsdManagerTool.setInfoCallBack(object : InfoCallBack {
            override fun msgCallback(list: List<NsdServiceInfo>) {
//                Log.d("xm--------list", String.valueOf(list?.size))

                for (i in list?.indices!!) {
                    val info = list[i]
                    info.serviceName
                    info.host
                    info.port
                    info.attributes
                    if (info.port > 0) {
                        searchList["${info.host.hostAddress}${info.port}"] = info
                    }

                    Log.d(
                        "xm--------list",
                        info.serviceName + " and host:${info.host.hostAddress}:${info.port}"
                    )
                }
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            nsdManagerTool.dispose()
        }
    }

    SearchDialog(viewModel, {
        nsdManagerTool.stop()
    }) {
        mConnectionInfo
    }

    ModalDrawerSheet(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .width(200.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        SearchButton(nsdManagerTool) { isSearch }
        Spacer(Modifier.height(12.dp))

        Box {
            SearchList({ searchList }) { it -> mConnectionInfo.value = it }
            if (searchList.size <= 0 && isSearch.value) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(64.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    trackColor = MaterialTheme.colorScheme.secondary,
                )
            }
        }
//        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//            Button(
//                modifier = Modifier.width(140.dp),
//                onClick = {
//                    if (!isSeach.value) {
//                        isSeach.value = true
//                        rememberCoroutineScope.launch(Dispatchers.IO) {
//                            //                        findBonjourTool.start()
//                        }
//                    } else {
//                        isSeach.value = false
//                        rememberCoroutineScope.launch {
//                            //                        findBonjourTool.stop()
//                        }
//                    }
//                },
//            ) {
//                Text(text = if (!isSeach.value) "开始搜索设备" else "停止搜索设备")
//            }
//        }
    }

//    Row {
//        Text(text = "1")
//        Text(text = "2")
//        Text(text = "3")
//        Box(modifier = Modifier.fillMaxWidth()) {
//            Button(
//                modifier = Modifier.width(140.dp),
//                onClick = {
//                    if (!isSeach.value) {
//                        isSeach.value = true
//                        rememberCoroutineScope.launch(Dispatchers.IO) {
//                            findBonjourTool.start()
//                        }
////                                        registerService()
//
////                                        nsdManger.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
//                    } else {
//                        isSeach.value = false
//                        rememberCoroutineScope.launch {
//                            findBonjourTool.stop()
//                        }
//                    }
//                },
//            ) {
//                Text(text = if (isSeach.value) "搜索中" else "搜索")
//            }
//        }
//    }
}

@Composable
fun SearchDialog(
    viewModel: MainViewModel,
    closeNsd: () -> Unit,
    connectionInfo: () -> MutableState<NsdServiceInfo?>
) {
    val connectionInfo = connectionInfo().value
    val scope = rememberCoroutineScope()
    when {
        connectionInfo != null -> {
            AlertDialogExample(
                onDismissRequest = {
                    connectionInfo().value = null
                },
                onConfirmation = {
                    connectionInfo().value = null
                    viewModel.closeDrawer(scope)
                    viewModel.connectionWebSocket(
                        connectionInfo.host.hostAddress,
                        connectionInfo.port
                    )
                    closeNsd()
                },
                dialogTitle = "连接设备",
                dialogText = "确定与\"${connectionInfo.serviceName}\"设备进行匹配！",
                icon = Icons.Default.Info
            )
        }
    }
}

@Composable
fun SearchButton(nsdManagerTool: NsdManagerTool, isSearch: () -> MutableState<Boolean>) {

    val scope = rememberCoroutineScope()
    val inversePrimary = MaterialTheme.colorScheme.inversePrimary
    val currentColor = LocalContentColor.current
    val getColor: () -> Color = { if (!isSearch().value) inversePrimary else currentColor }
    val getString: () -> String = { if (!isSearch().value) "开始搜索设备" else "停止搜索设备" }

    NavigationDrawerItem(
        icon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = getColor()
            )
        },
        label = { Text(getString()) },
        selected = isSearch().value,
        onClick = {
            scope.launch(Dispatchers.IO) {
//                                drawerState.close()
                if (isSearch().value) {
                    nsdManagerTool.start()
                } else {
                    nsdManagerTool.stop()
                }
            }
            isSearch().value = !isSearch().value
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedTextColor = MaterialTheme.colorScheme.inversePrimary,
            unselectedContainerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
fun SearchList(
    list: () -> MutableMap<String, NsdServiceInfo>,
    openDialog: (NsdServiceInfo) -> Unit
) {
    Toast.makeText(LocalContext.current, "size:${list().size}", Toast.LENGTH_LONG).show()
    LazyColumn(
        modifier = Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
            .padding(top = 5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val toList = list().toList()
        items(toList) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openDialog(it.second)
                    }
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = it.second.serviceName)
            }
            if (toList.last() != it) Divider()
        }
    }
}

@Composable
fun AlertDialogExample(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "设备连接确认提示")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText, textAlign = TextAlign.Center)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("连接")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("取消")
            }
        }
    )
}

