package com.zzx.common.socket

object ByteType {
    val BYTE_FILE_INIT: Byte = 0
    val BYTE_FILE_HEAD: Byte = 1
    val BYTE_FILE_READY: Byte = 2
    val BYTE_FILE_BODY: Byte = 3
    val BYTE_FILE_FINSH: Byte = 4
    val BYTE_FILE_END: Byte = 5
    val BYTE_FILE_ERROR: Byte = -1

    val MARK_READY = "READY"
    val MARK_END = "END"
    val MARK_FINSH = "FINSH"
}