package com.zzx.common.socket

import okio.buffer
import okio.sink
import okio.source
import java.net.ServerSocket
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import kotlin.concurrent.thread

actual object TurboFileHelper {

    actual fun startTurboServer(
        port: Int,
        channel: SeekableByteChannel,
        onReady: () -> Unit,
        onProgress: (Long) -> Unit,
        onComplete: () -> Unit
    ) {
        // 在新线程中启动服务端，不阻塞主协程协程调度 (因 ServerSocket.accept 是阻塞的)
        thread(start = true, name = "TurboServer", isDaemon = true) {
            println("[TurboServer] Starting on port $port...")
            
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(port)
                serverSocket.soTimeout = 10000 // 10秒内如果手机端没连上来，则自动关闭 (防呆机制)
                
                // 关键点：端口绑定成功，通知 UI/WebSocket 线程发送指令给手机
                onReady()

                serverSocket.accept().use { client ->
                    println("[TurboServer] Client connected: ${client.inetAddress}")
                    
                    val inputStream = Channels.newInputStream(channel)
                    val outputStream = client.getOutputStream()
                    
                    val buffer = ByteArray(64 * 1024)
                    var totalWritten = 0L
                    var read: Int
                    
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        totalWritten += read
                        onProgress(totalWritten)
                    }
                    outputStream.flush()
                    
                    println("[TurboServer] Stream completed successfully. Bytes sent: $totalWritten")
                }
            } catch (e: Exception) {
                println("[TurboServer] Error: ${e.message}")
            } finally {
                try {
                    serverSocket?.close()
                } catch (e: Exception) {}
                onComplete()
            }
        }
    }

    actual suspend fun receiveTurboFile(
        host: String,
        port: Int,
        targetPath: String,
        onProgress: (Long) -> Unit
    ): Boolean {
        // 桌面端作为发送方，通常不直接调用接收方法
        return false
    }
}
