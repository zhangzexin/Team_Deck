package com.zzx.common.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

interface FileMsgInterface {
    var state: Byte
    suspend fun dispacthFile(webSocket: WebSocket, bytes: ByteString)
}