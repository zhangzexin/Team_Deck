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


class WebSocketHandler constructor(var coroutineScope: CoroutineScope? = null) {

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
            Log.d(TAG, "onOpen: Success! Handshake complete.")
//            webSocket?.send("你好")
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
        }

        /**
         * Invoked when a web socket has been closed due to an error reading from or writing to the
         * network. Both outgoing and incoming messages may have been lost. No further calls to this
         * listener will be made.
         */
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "onFailure: Connection error! Response: $response, Error: $t")
            t.printStackTrace()
        }
    }

    fun connectionWebSocket(hostName: String, port: Int) {
        if (webSocket != null) {
            return
        }
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
            .pingInterval(30,TimeUnit.SECONDS) //心跳
            .readTimeout(30,TimeUnit.SECONDS)//设置读取超时时间
            .writeTimeout(30,TimeUnit.SECONDS)//设置写入超时时间
            .connectTimeout(3,TimeUnit.SECONDS)//设置连接超时时间
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