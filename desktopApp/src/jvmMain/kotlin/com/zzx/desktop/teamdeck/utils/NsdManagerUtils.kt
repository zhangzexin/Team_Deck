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
import com.zzx.desktop.teamdeck.ApplicationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okio.ByteString
import java.net.InetAddress
import javax.net.ssl.SSLSocketFactory

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class NsdManagerUtils {
    var mMockWebSocket: CMockWebServer? = null
    var coroutineScope: CoroutineScope? = null
    val mOutFileHandler = OutFileHandler()
    var mWebSocket: WebSocket? = null
    private var isSyncing = false

    // 连接状态与类型
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _connectionType = MutableStateFlow("未连接") // "有线 (USB)", "无线 (WiFi)", "未连接"
    val connectionType = _connectionType.asStateFlow()

    companion object {
        val Instance by lazy {
            NsdManagerUtils()
        }
    }

    val webSocketListener = object : WebSocketListener() {
        val gson = Gson()
        override fun onOpen(webSocket: WebSocket, response: Response) {
            mWebSocket = webSocket
            isSyncing = false // 为新会话重置同步状态
            
            // 判定连接类型 (ADB Reverse 通常会显示为 127.0.0.1)
            val host = response.request.url.host
            println("New connection from host: $host")
            
            if (host == "127.0.0.1" || host == "localhost") {
                _connectionType.value = "有线 (USB/ADB)"
            } else {
                _connectionType.value = "无线 (WiFi)"
            }
            _isConnected.value = true

            // 注入通用消息发送钩子
            com.zzx.common.plugin.PluginManager.globalMessageSender = { content ->
                mWebSocket?.send(content)
            }

            // 当 websocket 连接打开时，发送一条欢迎消息
            val initEvent = InitEvent(emptyList())
            val message = Message<InitEvent>(code = CodeEnum.INIT.value, msg = "连接成功", initEvent)
            val json = gson.toJson(message)
            mWebSocket?.send(json)

            // 【新增】发送同步清单：告知手机端当前 Desktop 上已加载的插件 ID 列表
            try {
                val installedIds = com.zzx.common.plugin.PluginManager.plugins.map { it.id }
                val syncEvent = com.zzx.common.socket.model.SyncPluginsEvent(installedIds)
                val syncMsg = Message(CodeEnum.SYNC_PLUGINS.value, "同步清单", syncEvent)
                mWebSocket?.send(gson.toJson(syncMsg))
                println("Sync: Sent authoritative plugin list to Android: $installedIds")
            } catch (e: Exception) {
                println("Error sending SYNC_PLUGINS: ${e.message}")
            }
            
            // 【核心优化】由原来的“全量推送”变为“按需同步”
            // 后续由 MessageHandler 在接收到 INITUI 后触发具体同步
            println("Handshake: Connection established. Waiting for Android inventory report...")
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
            _isConnected.value = false
            _connectionType.value = "未连接"
            coroutineScope?.launch(Dispatchers.IO) {
                mOutFileHandler.reset()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // 当 websocket 连接出现异常时，打印异常信息
            if (t is java.io.EOFException) {
                // 核心优化：针对 EOF (静默断开) 不打印大段堆栈，仅显示状态
                println("WebSocket status: Client Disconnected (EOF)")
            } else {
                println("WebSocket onFailure: ${t.toString()} ")
                t.printStackTrace()
            }
            _isConnected.value = false
            _connectionType.value = "未连接"
            coroutineScope?.launch(Dispatchers.IO) {
                mOutFileHandler.reset()
            }
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
        val lanAddress = NetUtils.getLocalHostAddress()
        // 绑定到 0.0.0.0 以支持 localhost (USB) 和局域网 (WiFi)
        mMockWebSocket?.start(java.net.InetAddress.getByName("0.0.0.0"), 0)
        
        // 【新增】设置 ADB 反向代理，支持有线连接 (映射到手机端 8888 端口)
        val actualPort = mMockWebSocket?.port ?: 0
        if (actualPort > 0) {
            com.zzx.desktop.teamdeck.utils.AdbUtils.ensureAdbAndReverse(actualPort)
        }

        // 获取用于广播的服务器 URL (使用局域网 IP)
        val serverUrl = "http://${lanAddress.hostAddress}:${actualPort}/"

        // 打印服务器的信息
        println("Server started on all interfaces (0.0.0.0) at port: $actualPort")
        println("Discovery URL (WiFi): $serverUrl")
        return mMockWebSocket!!
    }

    suspend fun sendFile(path: String) {
        mWebSocket?.let { mOutFileHandler.sendFile(path, it) }
    }

    /**
     * 【智能同步】对比手机端已有的插件 ID 列表，仅向手机发送其缺失的本地插件文件。
     */
    fun syncMissingPlugins(androidIds: List<String>) {
        coroutineScope?.launch(Dispatchers.IO) {
            println("Sync: Checking for missing plugins on Android (Android has: $androidIds)")
            
            // 获取本地所有加载成功的插件及其路径
            val desktopPlugins = com.zzx.common.plugin.PluginManager.plugins
            val toSync = desktopPlugins.filter { it.id !in androidIds }
            
            if (toSync.isEmpty()) {
                println("Sync: Android device is already up to date. No transfers needed.")
                return@launch
            }

            println("Sync: Found ${toSync.size} plugins to deploy: ${toSync.map { it.id }}")
            
            toSync.forEach { plugin ->
                // 从 PluginManager 注册表中获取该插件对应的物理文件路径
                val path = com.zzx.common.plugin.PluginManager.getPluginSourcePath(plugin.id)
                if (path != null && File(path).exists()) {
                    println("Sync: Pushing missing plugin file: $path")
                    sendFile(path)
                    delay(100) // 给予微小间隔防止 Socket 拥塞
                } else {
                    println("Sync: Skipping ${plugin.id} - Source file path not registered or missing.")
                }
            }
        }
    }

    fun stopMock() {
        coroutineScope?.cancel()
        coroutineScope = null
        mMockWebSocket?.close()
        mMockWebSocket = null
    }
}