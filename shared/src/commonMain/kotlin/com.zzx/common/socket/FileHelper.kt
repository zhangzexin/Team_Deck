package com.zzx.common.socket


import com.zzx.common.socket.ByteType.MARK_END
import com.zzx.common.socket.ByteType.MARK_READY
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import java.io.File
import java.nio.ByteBuffer

/**
 *@描述：
 *@time：2023/9/25
 *@author:zhangzexin
 */
object FileHelper {
    var byteBuffer:ByteBuffer? = null

    fun createFileDir(filePath:String):File {
        val dir = File(filePath)
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    suspend fun pushFileReady(webSocket: WebSocket) {
        var byteBuffer = ByteBuffer.allocate(4 * 1024)
        byteBuffer.put(ByteType.BYTE_FILE_READY)
        byteBuffer.put(MARK_READY.toByteArray())
        byteBuffer.flip()
        webSocket.send(byteBuffer.toByteString())
        byteBuffer.clear()
    }

    fun senFileEnd(webSocket: WebSocket) {
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocate(4*1024)
        }
        byteBuffer?.apply {
            put(ByteType.BYTE_FILE_END)
            put(MARK_END.toByteArray())
            flip()
            webSocket.send(this.toByteString())
        }
        close()
    }

    fun close() {
        byteBuffer?.clear()
        byteBuffer = null
    }
}