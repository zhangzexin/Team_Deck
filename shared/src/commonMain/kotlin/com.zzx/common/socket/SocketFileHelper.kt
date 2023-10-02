package com.zzx.common.socket

import com.zzx.common.ext.toJson
import com.zzx.common.socket.ByteType.MARK_FINSH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

object SocketFileHelper {
    val body = ByteBuffer.allocate(16 * 1024)
    var count = 0

    suspend fun pushFile(path: String, webSocket: WebSocket, changeEnd: () -> Unit) {
        withContext(Dispatchers.IO) {
            val file = File(path)
            val newByteChannel = Files.newByteChannel(file.toPath(), StandardOpenOption.READ)
            val body = ByteBuffer.allocate(16 * 1024)
            body.put(ByteType.BYTE_FILE_BODY)
            while (newByteChannel.read(body) > 0) {
                body.flip()
                webSocket.send(body.toByteString())
                body.clear()
                body.put(ByteType.BYTE_FILE_BODY)
                count += 1
                if (count>0 && count%20 == 0) {
                    delay(800)
                }
            }
            body.clear()
            body.put(ByteType.BYTE_FILE_FINSH)
            body.put(MARK_FINSH.toByteArray())
            body.flip()
            webSocket.send(body.toByteString())
            newByteChannel.close()
            changeEnd()
        }
    }

    suspend fun pushFileV2(newByteChannel: SeekableByteChannel, webSocket: WebSocket, changeEnd: () -> Unit) {
        withContext(Dispatchers.IO) {
            body.put(ByteType.BYTE_FILE_BODY)
            if (newByteChannel.read(body) > 0) {
                body.flip()
                webSocket.send(body.toByteString())
            } else {
                body.clear()
                body.put(ByteType.BYTE_FILE_FINSH)
                body.put(MARK_FINSH.toByteArray())
                body.flip()
                webSocket.send(body.toByteString())
                newByteChannel.close()
                changeEnd()
            }
            body.clear()
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