package com.zzx.common.socket

import java.nio.channels.SeekableByteChannel

/**
 * 极速 USB 传输助手 (Raw TCP)
 * 在 USB 连接下，绕开 WebSocket 协议栈，直接通过 Socket 流式传输文件。
 */
expect object TurboFileHelper {

    /**
     * [发送端 - 桌面] 启动临时 TCP 服务程序，等待接收端连接并推送文件流。
     * @param port 监听端口 (建议 54382)
     * @param channel 文件的读写通道
     * @param onReady 服务端已成功绑定端口并开始监听的回调
     * @param onComplete 传输完成 (无论成功失败) 的回调
     */
    fun startTurboServer(
        port: Int,
        channel: SeekableByteChannel,
        onReady: () -> Unit,
        onComplete: () -> Unit
    )

    /**
     * [接收端 - 安卓] 连接到桌面端的极速端口，并流式接收文件内容。
     * @param host 目标主机 (通常为 127.0.0.1)
     * @param port 目标端口
     * @param targetPath 本地保存路径
     * @return 是否传输成功
     */
    suspend fun receiveTurboFile(
        host: String,
        port: Int,
        targetPath: String
    ): Boolean
}
