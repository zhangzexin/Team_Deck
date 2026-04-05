package com.zzx.common.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.ByteArrayInputStream

/**
 * JVM (Desktop) 平台图像解码实现
 */
actual object ImageDecoder {
    actual fun decode(bytes: ByteArray): ImageBitmap? {
        return try {
            ByteArrayInputStream(bytes).use { 
                loadImageBitmap(it) 
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
