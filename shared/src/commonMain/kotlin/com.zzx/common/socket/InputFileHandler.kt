package com.zzx.common.socket

import com.zzx.common.ext.fromJson
import com.zzx.common.plugin.PluginLoader
import com.zzx.common.plugin.PluginManager
import com.zzx.common.socket.FileHelper.createFileDir
import com.zzx.common.socket.FileHelper.pushFileReady
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    var receivedBytes: Long = 0L
    private val writeLock = Mutex()

    companion object {
        public var plugindir: String? = null
        public var pluginLoader: PluginLoader? = null
    }

    override suspend fun dispacthFile(webSocket: WebSocket, bytes: ByteString) {
        writeLock.withLock {
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
                                    StandardOpenOption.WRITE,
                                    StandardOpenOption.TRUNCATE_EXISTING
                                )
                            }
                        }
                        val written = byteChannel?.write(ByteBuffer.wrap(newByteArray)) ?: 0
                        receivedBytes += written.toLong()
                        
                        // 计算并上报进度
                        val progress = if (fileInfo?.fileSize ?: 0L > 0) {
                            receivedBytes.toFloat() / fileInfo!!.fileSize.toFloat()
                        } else 0f
                        PluginManager.updateProgress(fileInfo!!.filename, progress)
                    }
                }

                ByteType.BYTE_FILE_FINSH -> {
                    val info = String(
                        byteBuffer.array(),
                        byteBuffer.position(),
                        byteBuffer.limit() - byteBuffer.position()
                    )
                    if (ByteType.MARK_FINSH == info) {
                        val fileName = fileInfo?.filename
                        val expectedSize = fileInfo?.fileSize ?: 0L
                        FileHelper.senFileEnd(webSocket)
                        val filePath = plugindir + "/" + fileName
                        val actualReceived = receivedBytes
                        val currentFileName = fileName // 捕获文件名
                        reset() // 必须先重置以关闭 byteChannel，确保文件完整落盘

                        // 传输完成，清理进度状态
                        currentFileName?.let { PluginManager.clearProgress(it) }

                        println("File received: $fileName, Size: $actualReceived/$expectedSize")

                        if (actualReceived == expectedSize && fileName != null && (fileName.endsWith(".apk") || fileName.endsWith(".aar") || fileName.endsWith(".jar"))) {
                            // 自动探测并加载插件 (使用 plugin.properties)
                            println("Android attempting auto-load for plugin: $fileName")
                            val loadedPlugin = pluginLoader?.loadPluginAuto(filePath)
                            
                            if (loadedPlugin != null) {
                                println("Android successfully auto-loaded plugin: ${loadedPlugin.name}")
                                PluginManager.addPlugin(loadedPlugin)
                            } else {
                                println("Android failed to auto-discover IPlugin in: $fileName")
                            }
                        } else if (actualReceived != expectedSize) {
                            println("File integrity check failed for $fileName!")
                        }
                    }
                }

                ByteType.BYTE_FILE_ERROR -> {
                    state = ByteType.BYTE_FILE_ERROR
                    fileInfo?.filename?.let { PluginManager.clearProgress(it) }
                    reset()
                }
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
        receivedBytes = 0L
    }

}