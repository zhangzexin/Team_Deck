package com.zzx.android.teamdeck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zzx.android.teamdeck.drawer.DrawerState
import com.zzx.android.teamdeck.drawer.DrawerValue
import com.zzx.android.teamdeck.socket.WebSocketHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 *@描述：
 *@time：2023/9/8
 *@author:zhangzexin
 */
class MainViewModel: ViewModel() {

    val webSocketHandler by lazy {
        WebSocketHandler(viewModelScope)
    }
    private var _drawerValue = MutableStateFlow(DrawerState(DrawerValue.Closed))
    val drawerValue = _drawerValue
    private var _isSearch = MutableStateFlow(false)
    val isSearch = _isSearch



    override fun onCleared() {
        super.onCleared()
        webSocketHandler.stop()
    }

    fun closeDrawer(scope: CoroutineScope) {
        scope.launch {
            _drawerValue.value.close()
        }
    }

    fun setSearchSwith(isOpen: Boolean) {
        _isSearch.value = isOpen
    }

    fun connectionWebSocket(hostName: String, port: Int) {
        webSocketHandler.connectionWebSocket(hostName, port)
    }

    fun sendMessage(msg:String) {
        webSocketHandler.sendMessage(msg)
    }

}