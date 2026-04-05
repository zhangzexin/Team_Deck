package com.zzx.android.teamdeck.socket

import android.util.Log
import com.zzx.android.teamdeck.socket.MessageHandler
import com.zzx.android.teamdeck.socket.ssl.RxUtils
import com.zzx.common.socket.InputFileHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Dns
import okhttp3.Protocol
import okio.ByteString
import java.net.Inet4Address
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory


import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSocketHandler constructor(var coroutineScope: CoroutineScope? = null) {
    private val _connectionSuccessEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val connectionSuccessEvent = _connectionSuccessEvent.asSharedFlow()
    
    private val _isConnectedFlow = MutableStateFlow(false)
    val isConnected = _isConnectedFlow.asStateFlow()
    
    private var _isConnected = false
    private val TAG: String = "TeamDeck-Net"
    private var webClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private val mInputFileHandler = InputFileHandler()

    private val socketlistener = object : WebSocketListener() {
        /**
         * Invoked when a web socket has been accepted by the remote peer and may begin transmitting
         * messages.
         */
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _isConnectedFlow.value = true
            _isConnected = true
            val host = response.request.url.host
            val isWired = host == "127.0.0.1" || host == "localhost"
            Log.d(TAG, "onOpen: Success! Connected to $host (Wired: $isWired)")
            
            // 注入通用消息发送钩子
            com.zzx.common.plugin.PluginManager.globalMessageSender = { content ->
                webSocket.send(content)
            }

            _connectionSuccessEvent.tryEmit(isWired)
        }

        /** Invoked when a text (type `0x1`) message has been received. */
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "onMessage (type `0x1`): $text")
            MessageHandler.dispatchersMsg(text)
        }

        /** Invoked when a binary (type `0x2`) message has been received. */
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "onMessage(type `0x2`): ")
            coroutineScope?.launch(Dispatchers.IO) {
                mInputFileHandler.dispacthFile(webSocket,bytes)
            }
        }

        /**
         * Invoked when the remote peer has indicated that no more incoming messages will be transmitted.
         */
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosing: code:$code reason:$reason")
        }

        /**
         * Invoked when both peers have indicated that no more messages will be transmitted and the
         * connection has been successfully released. No further calls to this listener will be made.
         */
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosed: code:$code reason:$reason")
            _isConnectedFlow.value = false
            _isConnected = false
            webSocket.close(1000, null)
        }

        /**
         * Invoked when a web socket has been closed due to an error reading from or writing to the
         * network. Both outgoing and incoming messages may have been lost. No further calls to this
         * listener will be made.
         */
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "onFailure: Connection error! Response: $response, Error: $t")
            t.printStackTrace()
            _isConnectedFlow.value = false
            _isConnected = false
        }
    }

    fun connect(hostName: String, port: Int) {
        Log.d(TAG, "Attempting connection to $hostName:$port")
        val factory: SSLSocketFactory? = RxUtils.createSSLSocketFactory()
        webClient = OkHttpClient.Builder()
            .dns(object : Dns {
                override fun lookup(hostname: String): List<java.net.InetAddress> {
                    // 强制只返回 IPv4 地址，彻底避开 IPv6 超时黑洞
                    return Dns.SYSTEM.lookup(hostname).filter { it is Inet4Address }
                }
            })
            .protocols(listOf(Protocol.HTTP_1_1)) // 锁定协议，跳过 HTTP/2 协商
            .retryOnConnectionFailure(true)//允许失败重试
            .pingInterval(30,TimeUnit.SECONDS) // 恢复到 30s，配合发送端的 Micro-Delay
            .readTimeout(90,TimeUnit.SECONDS)// 进一步放宽读取超时至 90s
            .writeTimeout(90,TimeUnit.SECONDS)// 进一步放宽写入超时至 90s
            .connectTimeout(10,TimeUnit.SECONDS)// 连接超时设置得更从容一些
            .build()
        val webSocketUrl = "ws://${hostName}:${port}"
        Log.d(TAG, "Initiating Connection to: $webSocketUrl")
        val request = Request.Builder().url(webSocketUrl).build()
        webSocket = webClient?.newWebSocket(request, socketlistener)

    }

    fun sendMessage(msg: String) {
        Log.d(TAG, "sendMessage: $msg")
        webSocket?.send(msg)
    }

    fun stop() {

    }
}