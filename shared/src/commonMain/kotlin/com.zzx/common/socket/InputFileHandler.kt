package com.zzx.common.socket

import com.zzx.common.ext.fromJson
import com.zzx.common.socket.FileHelper.createFileDir
import com.zzx.common.socket.FileHelper.pushFileReady
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.Sink
import okio.buffer
import okio.sink
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Arrays

/**
 * @描述:处理下载文件
 */
class InputFileHandler : FileMsgInterface {
    override var state = ByteType.BYTE_FILE_INIT
    var fileInfo: FileInfo? = null
    var byteChannel: ByteChannel? = null
    private val bufferSize = 4*1024
    var wBuffer = ByteBuffer.allocate(bufferSize)
    var mByteBuffer = ByteBuffer.allocate(4 * 1024)

    companion object {
        public var plugindir: String? = null
    }

    override suspend fun dispacthFile(webSocket: WebSocket, bytes: ByteString) {
        val byteBuffer = ByteBuffer.wrap(bytes.toByteArray())
        val mark = byteBuffer.get()
        val sliceBuffer = byteBuffer.slice()
        val newByteArray = ByteArray(sliceBuffer.remaining())
        sliceBuffer.get(newByteArray)
        println("mark:$mark")
        when (mark) {
            ByteType.BYTE_FILE_HEAD -> {
                if (state == ByteType.BYTE_FILE_INIT) {
                    state = ByteType.BYTE_FILE_HEAD
//                    val info = String(
//                        byteBuffer.array(),
//                        byteBuffer.position(),
//                        byteBuffer.limit() - byteBuffer.position()
//                    )

                    val info = String(newByteArray)
                    fileInfo = info.fromJson<FileInfo>()
                    plugindir?.let {
                        createFileDir(it)
                        pushFileReady(webSocket)
                        state = ByteType.BYTE_FILE_READY
                    }
                }
            }

            ByteType.BYTE_FILE_BODY -> {
                if ((state == ByteType.BYTE_FILE_READY || state == ByteType.BYTE_FILE_BODY) && fileInfo != null) {
                    state = ByteType.BYTE_FILE_BODY
                    if (byteChannel == null) {
                        byteChannel = withContext(Dispatchers.IO) {
                            Files.newByteChannel(
                                File(plugindir + "/" + fileInfo!!.filename).toPath(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE
                            )
                        }
                    }
                        byteChannel?.write(ByteBuffer.wrap(newByteArray))
//                        webSocket.send("")
                        pushFileReady(webSocket)
                }
            }

            ByteType.BYTE_FILE_FINSH -> {
                val info = String(
                    byteBuffer.array(),
                    byteBuffer.position(),
                    byteBuffer.limit() - byteBuffer.position()
                )
                if (ByteType.MARK_FINSH == info) {
                    state = ByteType.BYTE_FILE_FINSH
                    FileHelper.senFileEnd(webSocket)
                    reset()
                }
            }

            ByteType.BYTE_FILE_ERROR -> {
                state = ByteType.BYTE_FILE_ERROR
                reset()
            }
        }
    }

    fun InitDir(path: String) {
        plugindir = path
    }

    fun reset() {
        state = ByteType.BYTE_FILE_INIT
        byteChannel = byteChannel?.run {
            close()
            null
        }
        fileInfo = null
        plugindir = null
    }

}