package com.zzx.desktop.teamdeck.utils

import com.google.gson.Gson
import com.zzx.common.socket.OutFileHandler
import com.zzx.common.socket.model.InitEvent
import com.zzx.common.socket.model.Message
import com.zzx.common.socket.type.CodeEnum
import com.zzx.desktop.teamdeck.socket.CMockWebServer
import com.zzx.desktop.teamdeck.socket.MdnsUtils
import com.zzx.desktop.teamdeck.socket.MessageHandler
import com.zzx.desktop.teamdeck.socket.ssl.RxUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okio.ByteString
import java.net.InetAddress
import javax.net.ssl.SSLSocketFactory

class NsdManagerUtils {
    var mMockWebSocket: CMockWebServer? = null
    var coroutineScope: CoroutineScope? = null
    val mOutFileHandler = OutFileHandler()
    var mWebSocket: WebSocket? = null


    companion object {
        val Instance by lazy {
            NsdManagerUtils()
        }
    }

    val webSocketListener = object : WebSocketListener() {
        val gson = Gson()
        override fun onOpen(webSocket: WebSocket, response: Response) {
            mWebSocket = webSocket
            // 当 websocket 连接打开时，发送一条欢迎消息
            val initEvent = InitEvent(emptyList())
            val message = Message<InitEvent>(code = CodeEnum.INIT.value, msg = "连接成功", initEvent)
            val json = gson.toJson(message)
            mWebSocket?.send(json)
            MdnsUtils.instance.stop()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // 当收到客户端发来的消息时，打印消息内容，并回复一条确认消息
            println("Received message from client: $text")
            coroutineScope?.let { MessageHandler.dispatchMsg(it, text) }
//            webSocket.send("Message received!")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            coroutineScope?.launch(Dispatchers.IO) {
                mOutFileHandler.dispacthFile(webSocket, bytes)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            // 客户端主动关闭时回调
            println("onClosing")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // 当 websocket 连接关闭时，打印关闭原因
            println("WebSocket closed: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // 当 websocket 连接出现异常时，打印异常信息
            println("WebSocket onFailure: ${t.toString()} ")
            t.printStackTrace()
        }
    }


    fun mockWebSocket(coroutineScope: CoroutineScope): CMockWebServer {
        this.coroutineScope = coroutineScope
        if (mMockWebSocket != null) {
            return mMockWebSocket!!
        }
        mMockWebSocket = CMockWebServer()
        val factory: SSLSocketFactory? = RxUtils.createSSLSocketFactory()
        // 使用系统默认的证书
        //    val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory // 创建 sslSocketFactory
        //
        ////    val trustManager =
        ////        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).trustManagers[0] as X509TrustManager
        //    mMockWebSocket?.useHttps(factory!!,true)
        // 创建一个 WebSocketListener 对象，用于监听 websocket 事件

        //    mMockWebSocket?.enqueue(MockResponse().withWebSocketUpgrade(webSocketListener))

        // 创建一个 MockResponse 对象，用于响应 websocket 握手请求
        val mockResponse = MockResponse()
            .setResponseCode(101) // 设置响应码为 101 ，表示切换协议
            .addHeader("Upgrade", "websocket") // 设置 Upgrade 头部为 websocket ，表示切换到 websocket 协议
            .addHeader("Connection", "Upgrade") // 设置 Connection 头部为 Upgrade ，表示保持连接
            .withWebSocketUpgrade(webSocketListener) // 设置 websocket 升级监听器

        // 将响应添加到队列中
        mMockWebSocket?.enqueue(mockResponse)
        mMockWebSocket?.start(InetAddress.getByName(InetAddress.getLocalHost().hostAddress), 0)
        // 获取服务器的 URL
        val serverUrl = mMockWebSocket?.url("/")

        // 打印服务器的 URL
        println("Server URL: $serverUrl")
        return mMockWebSocket!!
    }

    suspend fun sendFile(path: String) {
        mWebSocket?.let { mOutFileHandler.sendFile(path, it) }
    }

    fun stopMock() {
        coroutineScope?.cancel()
        coroutineScope = null
        mMockWebSocket?.close()
        mMockWebSocket = null
    }
}