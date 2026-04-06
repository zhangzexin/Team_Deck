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

    suspend fun pushFileReady(webSocket: WebSocket, transferId: Int) {
        val byteBuffer = ByteBuffer.allocate(4 * 1024)
        byteBuffer.put(ByteType.BYTE_FILE_READY)
        byteBuffer.putInt(transferId)
        byteBuffer.put(MARK_READY.toByteArray())
        byteBuffer.flip()
        webSocket.send(byteBuffer.toByteString())
    }

    fun senFileEnd(webSocket: WebSocket, transferId: Int) {
        val byteBuffer = ByteBuffer.allocate(4 * 1024)
        byteBuffer.put(ByteType.BYTE_FILE_END)
        byteBuffer.putInt(transferId)
        byteBuffer.put(MARK_END.toByteArray())
        byteBuffer.flip()
        webSocket.send(byteBuffer.toByteString())
    }

    fun close() {
        byteBuffer?.clear()
        byteBuffer = null
    }
}