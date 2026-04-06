package com.zzx.common.socket

import com.zzx.common.socket.ByteType.MARK_END
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

/**
 * @描述: 处理上传文件 (支持多路复用并发传输)
 */
class OutFileHandler : FileMsgInterface {
    override var state = ByteType.BYTE_FILE_INIT
    
    // 任务状态模�?
    private class OutTask(
        val pluginpath: String,
        val inputByteChannel: SeekableByteChannel,
        var state: Byte
    )

    private val tasks = java.util.concurrent.ConcurrentHashMap<Int, OutTask>()

    override suspend fun dispacthFile(webSocket: WebSocket, bytes: ByteString) {
        val byteBuffer = ByteBuffer.wrap(bytes.toByteArray())
        val mark = byteBuffer.get()
        
        // 核心改动：从协议包中提取 4 字节�?TransferID
        if (byteBuffer.remaining() < 4) return
        val transferId = byteBuffer.getInt()
        
        val task = tasks[transferId] ?: return

        println("Mark: $mark for TransferID: $transferId")
        when (mark) {
            ByteType.BYTE_FILE_READY -> {
                println("BYTE_FILE_READY for $transferId")
                if (task.state == ByteType.BYTE_FILE_HEAD || task.state == ByteType.BYTE_FILE_READY) {
                    task.state = ByteType.BYTE_FILE_READY
                    
                    // [核心改进] 发送剩余数�?
                    // 这里由于是并发环境，我们直接在协程中驱动发�?
                    SocketFileHelper.pushFileV2(task.inputByteChannel, webSocket, transferId) {
                        task.state = ByteType.BYTE_FILE_FINSH
                    }
                }
            }

            ByteType.BYTE_FILE_END -> {
                println("BYTE_FILE_END for $transferId")
                withContext(Dispatchers.IO) {
                    task.inputByteChannel.close()
                }
                tasks.remove(transferId)
                // 如果所有任务都结束了，将全局状态置�?INIT
                if (tasks.isEmpty()) state = ByteType.BYTE_FILE_INIT
            }

            ByteType.BYTE_FILE_ERROR -> {
                println("BYTE_FILE_ERROR received for $transferId.")
                withContext(Dispatchers.IO) {
                    task.inputByteChannel.close()
                }
                tasks.remove(transferId)
                if (tasks.isEmpty()) state = ByteType.BYTE_FILE_INIT
            }
        }
    }

    // 发送指定文�?(并发调用安全)
    suspend fun sendFile(path: String, webSocket: WebSocket) {
        val transferId = path.hashCode()
        
        // 如果该文件已经在传，先取消旧任务
        tasks[transferId]?.let { oldTask ->
            withContext(Dispatchers.IO) { oldTask.inputByteChannel.close() }
            tasks.remove(transferId)
        }

        println("Initiating multiplexed transfer for: $path (ID: $transferId)")
        val channel = SocketFileHelper.pushFileInof(path, webSocket, transferId)
        val newTask = OutTask(path, channel, ByteType.BYTE_FILE_HEAD)
        tasks[transferId] = newTask
        state = ByteType.BYTE_FILE_BODY // 表示当前有活跃任�?
    }

    suspend fun reset() {
        withContext(Dispatchers.IO) {
            tasks.forEach { (_, task) ->
                try {
                    task.inputByteChannel.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        tasks.clear()
        state = ByteType.BYTE_FILE_INIT
    }
}
