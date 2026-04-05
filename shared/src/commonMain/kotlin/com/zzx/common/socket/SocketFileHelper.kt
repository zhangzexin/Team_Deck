package com.zzx.common.socket

import com.zzx.common.ext.toJson
import com.zzx.common.socket.ByteType.MARK_FINSH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

object SocketFileHelper {
    var count = 0

    suspend fun pushFile(path: String, webSocket: WebSocket, changeEnd: () -> Unit) {
        withContext(Dispatchers.IO) {
            val file = File(path)
            val newByteChannel = Files.newByteChannel(file.toPath(), StandardOpenOption.READ)
            val bufferSize = 64 * 1024
            val body = ByteBuffer.allocate(bufferSize)
            
            println("Standard push started: ${file.name} (${file.length()} bytes) using 64KB chunks")
            try {
                var chunkCount = 0
                while (newByteChannel.read(body) > 0) {
                    body.flip()
                    // 构造：[BODY_TYPE][DATA...]
                    val frame = ByteBuffer.allocate(body.remaining() + 1)
                    frame.put(ByteType.BYTE_FILE_BODY)
                    frame.put(body)
                    frame.flip()
                    
                    if (!webSocket.send(frame.toByteString())) break
                    
                    body.clear()
                    chunkCount++
                    
                    // 流量节流阀 (Flow Control): 监控 WebSocket 内部发送队列水位
                    // 如果积压超过 2MB，则暂停发送，防止缓冲区溢出并腾出带宽给的心跳 Pong 响应
                    while (webSocket.queueSize() > 2 * 1024 * 1024) {
                        println("Wait for network... Current queue: ${webSocket.queueSize() / 1024} KB")
                        delay(100)
                        yield()
                    }

                    // 即使没有积压，每发送 1.2MB 也微呼吸一次，确保 CPU 轮转
                    if (chunkCount % 20 == 0) {
                        delay(2)
                        yield()
                    }
                }
                
                // 确保数据刷新与完成帧发送
                delay(200)
                val finishFrame = ByteBuffer.allocate(MARK_FINSH.toByteArray().size + 1)
                finishFrame.put(ByteType.BYTE_FILE_FINSH)
                finishFrame.put(MARK_FINSH.toByteArray())
                finishFrame.flip()
                webSocket.send(finishFrame.toByteString())
                println("Push completed successfully.")
            } catch (e: Exception) {
                println("Push interrupted: ${e.message}")
            } finally {
                newByteChannel.close()
                changeEnd()
            }
        }
    }

    suspend fun pushFileV2(newByteChannel: SeekableByteChannel, webSocket: WebSocket, changeEnd: () -> Unit) {
        withContext(Dispatchers.IO) {
            val bufferSize = 64 * 1024
            val body = ByteBuffer.allocate(bufferSize)
            
            println("Streaming start: Full file push initiated using 64KB buffers.")
            try {
                while (newByteChannel.read(body) > 0) {
                    body.flip()
                    // 构造数据帧：[MARK_BODY][DATA...]
                    val frame = ByteBuffer.allocate(body.remaining() + 1)
                    frame.put(ByteType.BYTE_FILE_BODY)
                    frame.put(body)
                    frame.flip()
                    
                    if (!webSocket.send(frame.toByteString())) {
                        println("WebSocket buffer full or closed. Streaming interrupted.")
                        break
                    }
                    body.clear()
                    yield() // 让出 CPU，处理心跳与控制帧
                }
                
                // 发送结束帧
                // 增加少量延迟，确保所有数据包在连接状态改变前已送达接收端缓冲区
                delay(200)
                val finishFrame = ByteBuffer.allocate(MARK_FINSH.toByteArray().size + 1)
                finishFrame.put(ByteType.BYTE_FILE_FINSH)
                finishFrame.put(MARK_FINSH.toByteArray())
                finishFrame.flip()
                if (webSocket.send(finishFrame.toByteString())) {
                    println("Streaming finish: File sent successfully.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                newByteChannel.close()
                changeEnd()
            }
        }
    }

    suspend fun pushFileInof(filePath: String, webSocket: WebSocket): SeekableByteChannel {
        val file = File(filePath)
        var byteBuffer = ByteBuffer.allocate(4 * 1024)
        byteBuffer.put(ByteType.BYTE_FILE_HEAD)
        val json = FileInfo(file.name, file.length(), "").toJson()
        byteBuffer.put(json.toByteArray())
        byteBuffer.flip()
        webSocket.send(byteBuffer.toByteString())
        byteBuffer.clear()
        return withContext(Dispatchers.IO) {
            Files.newByteChannel(file.toPath(), StandardOpenOption.READ)
        }
    }
}