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
                    inputByteChannel?.apply {
                        state = ByteType.BYTE_FILE_READY

                        // [核心改进] 优先尝试开启极速涡轮 (Raw TCP) 模式
                        // 桌面端作为 Server 启动监听，通知安卓端连接 localhost:54382 (adb reverse)
                        println("[OutFileHandler] Initiating Turbo USB Push...")
                        val turboPort = 54382
                        
                        TurboFileHelper.startTurboServer(
                            port = turboPort, 
                            channel = this,
                            onReady = {
                                // 关键点：只有当 Server 真正监听好了，才发指令给手机
                                println("[OutFileHandler] Turbo Server Ready. Sending start command...")
                                val turboStartFrame = ByteBuffer.allocate(5)
                                turboStartFrame.put(ByteType.BYTE_FILE_TURBO_START)
                                turboStartFrame.putInt(turboPort)
                                turboStartFrame.flip()
                                webSocket.send(ByteString.of(*turboStartFrame.array()))
                            },
                            onComplete = {
                                if (state != ByteType.BYTE_FILE_FINSH) {
                                    // 如果涡轮结束时状态不是 FINSH，说明可能连接失败了
                                    println("[OutFileHandler] Turbo server closed without success.")
                                } else {
                                    state = ByteType.BYTE_FILE_FINSH
                                }
                            }
                        )
                    }
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
                println("BYTE_FILE_ERROR received.")
                if (state == ByteType.BYTE_FILE_READY && inputByteChannel != null) {
                    // [核心改进] 涡轮模式失败后的自动回退机制
                    // 如果手机端报错（通常是连不上 54382 端口），我们回退到稳健的 WebSocket 模式
                    println("[OutFileHandler] Turbo mode failed. Falling back to WebSocket...")
                    SocketFileHelper.pushFileV2(inputByteChannel!!, webSocket) {
                        state = ByteType.BYTE_FILE_FINSH
                    }
                } else {
                    state = ByteType.BYTE_FILE_ERROR
                    withContext(Dispatchers.IO) {
                        inputByteChannel?.close()
                    }
                    state = ByteType.BYTE_FILE_INIT
                }
            }
        }
    }


    //发送指定文件
    suspend fun sendFile(path: String, webSocket: WebSocket) {
        if (state != ByteType.BYTE_FILE_INIT) {
            println("OutFileHandler: Auto-resetting stale state ($state)")
            reset()
        }
        
        state = ByteType.BYTE_FILE_HEAD
        pluginpath = path
        inputByteChannel = SocketFileHelper.pushFileInof(path, webSocket)
    }

    private suspend fun reset() {
        withContext(Dispatchers.IO) {
            try {
                inputByteChannel?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        inputByteChannel = null
        pluginpath = null
        state = ByteType.BYTE_FILE_INIT
    }
}