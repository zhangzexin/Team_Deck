package com.zzx.common.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 跨平台图像解码器
 */
expect object ImageDecoder {
    /**
     * 将字节数组解码为 ImageBitmap
     */
    fun decode(bytes: ByteArray): ImageBitmap?
}
