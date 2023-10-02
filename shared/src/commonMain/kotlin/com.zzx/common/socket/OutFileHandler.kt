package com.zzx.common.socket

import com.zzx.common.socket.ByteType.MARK_END
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

/**
 * @描述: 处理上传文件
 */
class OutFileHandler : FileMsgInterface {
    override var state = ByteType.BYTE_FILE_INIT
    var inputByteChannel: SeekableByteChannel? = null
    var pluginpath: String? = null
    override suspend fun dispacthFile(webSocket: WebSocket, bytes: ByteString) {
        val byteBuffer = ByteBuffer.wrap(bytes.toByteArray())
        val mark = byteBuffer[0]
        println("mark:$mark")
        when (mark) {
            ByteType.BYTE_FILE_READY -> {
                println("BYTE_FILE_READY")
                if (state == ByteType.BYTE_FILE_HEAD || state == ByteType.BYTE_FILE_READY) {
//                        val body = ByteBuffer.allocate(16 * 1024)
//                        if (read(body) > 0) {
//                            body.put(ByteType.BYTE_FILE_BODY)
//                            body.flip()
//                            webSocket.send(body!!.toByteString())
//                            body.clear()
//                        } else {
//                            close()
//                            state = ByteType.BYTE_FILE_FINSH
//                            body.clear()
//                            body.put(ByteType.BYTE_FILE_FINSH)
//                            body.put(ByteType.MARK_FINSH.toByteArray())
//                            body.flip()
//                            webSocket.send(body.toByteString())
//                        }
                    inputByteChannel?.apply {
                        state = ByteType.BYTE_FILE_READY
                        SocketFileHelper.pushFileV2(this, webSocket) {
                            state = ByteType.BYTE_FILE_FINSH
                        }
                    }

//                    pluginpath?.let {
//                        state = ByteType.BYTE_FILE_READY
//                        SocketFileHelper.pushFile(it, webSocket) {
//                            state = ByteType.BYTE_FILE_FINSH
//                        }
//                    }
                }
            }

            ByteType.BYTE_FILE_END -> {
                println("BYTE_FILE_END")
                byteBuffer.get()
                val string = String(byteBuffer.array())
                if (MARK_END == string) {
                    withContext(Dispatchers.IO) {
                        inputByteChannel?.close()
                    }
                    inputByteChannel = null
                    state = ByteType.BYTE_FILE_INIT
                }
            }

            ByteType.BYTE_FILE_ERROR -> {
                println("BYTE_FILE_ERROR")
                state = ByteType.BYTE_FILE_ERROR
                withContext(Dispatchers.IO) {
                    inputByteChannel?.close()
                }
                state = ByteType.BYTE_FILE_INIT
            }
        }
    }


    //发送指定文件
    suspend fun sendFile(path: String, webSocket: WebSocket) {
        if (state == ByteType.BYTE_FILE_INIT) {
            state = ByteType.BYTE_FILE_HEAD
            pluginpath = path
            inputByteChannel = SocketFileHelper.pushFileInof(path, webSocket)
        } else {
            throw IllegalStateException("request reset OutFileHandler state")
        }
    }
}