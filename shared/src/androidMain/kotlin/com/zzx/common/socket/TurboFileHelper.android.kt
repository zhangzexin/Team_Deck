package com.zzx.common.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.SeekableByteChannel

actual object TurboFileHelper {

    actual fun startTurboServer(
        port: Int,
        channel: SeekableByteChannel,
        onReady: () -> Unit,
        onComplete: () -> Unit
    ) {
        // 安卓端通常作为接收方，不直接启动服务口
    }

    actual suspend fun receiveTurboFile(
        host: String,
        port: Int,
        targetPath: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            println("[TurboClient] Connecting to $host:$port...")
            try {
                Socket().use { socket ->
                    // 手机端连接到 127.0.0.1:54382 (需 adb reverse 配合)
                    socket.connect(InetSocketAddress(host, port), 5000)
                    println("[TurboClient] Connected to server.")

                    val targetFile = File(targetPath)
                    if (!targetFile.exists()) {
                        targetFile.parentFile?.mkdirs()
                        targetFile.createNewFile()
                    }

                    val source = socket.getInputStream().source().buffer()
                    val sink = targetFile.sink().buffer()

                    println("[TurboClient] Receiving data...")
                    val totalRead = sink.writeAll(source)
                    sink.flush()
                    
                    println("[TurboClient] File received: $targetPath, Bytes: $totalRead")
                    totalRead > 0
                }
            } catch (e: Exception) {
                println("[TurboClient] Connection failed: ${e.message}")
                false
            }
        }
    }
}
