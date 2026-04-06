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
 * @描述:处理�?**
 * @描述: 处理下载文件 (支持多路复用并发传输)
 */
class InputFileHandler : FileMsgInterface {
    override var state = ByteType.BYTE_FILE_INIT
    
    // 内部任务模型
    private class InTask(
        var fileInfo: FileInfo? = null,
        var byteChannel: ByteChannel? = null,
        var receivedBytes: Long = 0L,
        var state: Byte = ByteType.BYTE_FILE_INIT
    )

    private val tasks = java.util.concurrent.ConcurrentHashMap<Int, InTask>()
    private val writeLock = Mutex()

    companion object {
        var plugindir: String? = null
        var pluginLoader: PluginLoader? = null
    }

    override suspend fun dispacthFile(webSocket: WebSocket, bytes: ByteString) {
        val byteBuffer = ByteBuffer.wrap(bytes.toByteArray())
        val mark = byteBuffer.get()
        
        // 核心改动：从头部提取 4 字节�?TransferID
        if (byteBuffer.remaining() < 4) return
        val transferId = byteBuffer.getInt()
        
        // 获取或创建任�?
        val task = tasks.getOrPut(transferId) { InTask() }

        val remainingBytes = byteBuffer.remaining()
        val dataArray = ByteArray(remainingBytes)
        byteBuffer.get(dataArray)

        writeLock.withLock {
            when (mark) {
                ByteType.BYTE_FILE_HEAD -> {
                    val info = String(dataArray)
                    val newFileInfo = info.fromJson<FileInfo>()
                    
                    // 去重逻辑：如果该任务已处于接收状态且信息一致，则不再触发 UI 初始化
                    if (task.state >= ByteType.BYTE_FILE_READY && task.fileInfo?.filename == newFileInfo.filename) {
                        return@withLock Unit
                    }

                    task.state = ByteType.BYTE_FILE_HEAD
                    task.fileInfo = newFileInfo
                    plugindir?.let {
                        createFileDir(it)
                        pushFileReady(webSocket, transferId)
                        task.state = ByteType.BYTE_FILE_READY
                        // 立即上报 0% 进度
                        task.fileInfo?.filename?.let { name ->
                            PluginManager.updateProgress(name, 0.0f)
                        }
                    }
                    Unit
                }

                ByteType.BYTE_FILE_BODY -> {
                    if ((task.state == ByteType.BYTE_FILE_READY || task.state == ByteType.BYTE_FILE_BODY) && task.fileInfo != null) {
                        task.state = ByteType.BYTE_FILE_BODY
                        if (task.byteChannel == null) {
                            task.byteChannel = withContext(Dispatchers.IO) {
                                Files.newByteChannel(
                                    File(plugindir + "/" + task.fileInfo!!.filename).toPath(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE,
                                    StandardOpenOption.TRUNCATE_EXISTING
                                )
                            }
                        }
                        val written = task.byteChannel?.write(ByteBuffer.wrap(dataArray)) ?: 0
                        task.receivedBytes += written.toLong()
                        
                        // 计算并上报进�?
                        val progress = if (task.fileInfo?.fileSize ?: 0L > 0) {
                            task.receivedBytes.toFloat() / task.fileInfo!!.fileSize.toFloat()
                        } else 0f
                        PluginManager.updateProgress(task.fileInfo!!.filename, progress)
                    }
                }

                ByteType.BYTE_FILE_FINSH -> {
                    val info = String(dataArray)
                    if (ByteType.MARK_FINSH == info) {
                        val fileName = task.fileInfo?.filename
                        val expectedSize = task.fileInfo?.fileSize ?: 0L
                        val actualReceived = task.receivedBytes
                        val filePath = plugindir + "/" + fileName
                        
                        // 确认回复
                        FileHelper.senFileEnd(webSocket, transferId)
                        
                        // 清理任务
                        task.byteChannel?.close()
                        tasks.remove(transferId)
                        if (tasks.isEmpty()) state = ByteType.BYTE_FILE_INIT

                        // 传输完成，清理进度状�?
                        fileName?.let { PluginManager.clearProgress(it) }

                        println("File received: $fileName, Size: $actualReceived/$expectedSize")

                        if (actualReceived == expectedSize && fileName != null && (fileName.endsWith(".apk") || fileName.endsWith(".aar") || fileName.endsWith(".jar"))) {
                            println("Android attempting auto-load for plugin: $fileName")
                            val loadedPlugin = pluginLoader?.loadPluginAuto(filePath)
                            if (loadedPlugin != null) {
                                println("Android successfully auto-loaded plugin: ${loadedPlugin.name}")
                                PluginManager.addPlugin(loadedPlugin)
                            }
                        }
                    }
                }

                ByteType.BYTE_FILE_ERROR -> {
                    task.byteChannel?.close()
                    task.fileInfo?.filename?.let { PluginManager.clearProgress(it) }
                    tasks.remove(transferId)
                    if (tasks.isEmpty()) state = ByteType.BYTE_FILE_INIT
                }
            }
            Unit
        }
    }

    fun InitDir(path: String) {
        plugindir = path
    }

    fun reset() {
        tasks.forEach { (_, task) ->
            try {
                task.byteChannel?.close()
            } catch (e: Exception) {}
        }
        tasks.clear()
        state = ByteType.BYTE_FILE_INIT
    }
}
