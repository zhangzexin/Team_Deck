package com.zzx.desktop.teamdeck.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

/**
 *@描述：文件管理类
 *@time：2023/10/2
 *@author:zhangzexin
 */
object FileUtils {

    /**
     * 快速复制文件
     */
    suspend fun copyFileTo(source: String,target: String) {
        val fileSource = File(source)
        createDirs(target)
        val fileTarget = File(target+"/"+fileSource.toPath().fileName)
        withContext(Dispatchers.IO) {
            Files.copy(fileSource.toPath(), fileTarget.toPath())
        }
    }

    /**
     * 创建目录
     */
    fun createDirs(path:String){
        val file = File(path)
        if (!file.isDirectory) {
            file.mkdirs()
        }
    }
}