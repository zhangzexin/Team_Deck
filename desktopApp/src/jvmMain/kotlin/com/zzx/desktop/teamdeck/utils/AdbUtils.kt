package com.zzx.desktop.teamdeck.utils

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/**
 * ADB 工具类：支持系统探测与内置二进制提取
 */
object AdbUtils {
    private var adbPath: String? = null
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private val isMac = System.getProperty("os.name").lowercase().contains("mac")
    
    /**
     * 确保 adb 可用，并为所有已连接设备执行端口反向代理 (adb reverse)
     */
    fun ensureAdbAndReverse(pcPort: Int) {
        val path = findOrExtractAdb()
        if (path == null) {
            println("Wired Connection Notice: ADB environment not found. Using WiFi only.")
            return
        }
        
        println("Using ADB at: $path")
        
        // 尝试获取设备列表并为每个设备执行 reverse
        try {
            val devices = getConnectedDevices(path)
            if (devices.isEmpty()) {
                println("Wired Connection Notice: No USB devices found.")
                // 即使没发现设备，也尝试对默认设备执行一次（兼容某些情况）
                runAdbCommand(path, "reverse tcp:8888 tcp:$pcPort")
            } else {
                devices.forEach { deviceId ->
                    println("Setting up wired connection for device: $deviceId")
                    runAdbCommand(path, "-s $deviceId reverse tcp:8888 tcp:$pcPort")
                }
            }
        } catch (e: Exception) {
            // 回退到简单模式
            runAdbCommand(path, "reverse tcp:8888 tcp:$pcPort")
        }
    }

    private fun findOrExtractAdb(): String? {
        if (adbPath != null) return adbPath

        // 1. 尝试系统环境变量中的 adb
        val systemAdb = if (isWindows) "adb.exe" else "adb"
        if (commandExists(systemAdb)) {
            adbPath = systemAdb
            return adbPath
        }

        // 2. 尝试常见安装路径
        val commonPaths = if (isWindows) {
            listOf(
                System.getenv("ANDROID_HOME")?.let { "$it\\platform-tools\\adb.exe" },
                System.getenv("LOCALAPPDATA")?.let { "$it\\Android\\Sdk\\platform-tools\\adb.exe" }
            )
        } else {
            listOf(
                System.getenv("ANDROID_HOME")?.let { "$it/platform-tools/adb" },
                File(System.getProperty("user.home"), "Library/Android/sdk/platform-tools/adb").absolutePath,
                "/usr/local/bin/adb",
                "/usr/bin/adb"
            )
        }

        for (p in commonPaths) {
            if (p != null && File(p).exists()) {
                adbPath = p
                return adbPath
            }
        }

        // 3. 尝试从内置资源提取 (离线支持)
        val extractedAdb = extractBundledAdb()
        if (extractedAdb != null && extractedAdb.exists()) {
            adbPath = extractedAdb.absolutePath
            return adbPath
        }

        return null
    }

    private fun commandExists(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(command, "version"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun extractBundledAdb(): File? {
        val osDir = when {
            isWindows -> "windows"
            isMac -> "macos"
            else -> "linux"
        }
        
        val exeName = if (isWindows) "adb.exe" else "adb"
        val resourcePath = "/bin/$osDir/$exeName"
        val inputStream = javaClass.getResourceAsStream(resourcePath) ?: return null

        val destDir = File(System.getProperty("user.home"), ".teamdeck/bin")
        if (!destDir.exists()) destDir.mkdirs()

        val destFile = File(destDir, exeName)
        
        // Windows 依赖 DLL
        if (isWindows) {
            extractResource("/bin/windows/AdbWinApi.dll", File(destDir, "AdbWinApi.dll"))
            extractResource("/bin/windows/AdbWinUsbApi.dll", File(destDir, "AdbWinUsbApi.dll"))
        }

        return try {
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // 赋予执行权限
            if (!isWindows) {
                try {
                    val permissions = setOf(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
                    )
                    Files.setPosixFilePermissions(destFile.toPath(), permissions)
                } catch (e: Exception) {
                    destFile.setExecutable(true)
                }
            }
            destFile
        } catch (e: Exception) {
            null
        }
    }

    private fun extractResource(resourcePath: String, destFile: File) {
        try {
            javaClass.getResourceAsStream(resourcePath)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun getConnectedDevices(adb: String): List<String> {
        val devices = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec("$adb devices")
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.endsWith("device")) {
                        devices.add(line.split("\t")[0])
                    }
                }
            }
        } catch (e: Exception) { /* ignore */ }
        return devices
    }

    private fun runAdbCommand(adb: String, command: String) {
        try {
            val fullCommand = "\"$adb\" $command"
            val process = Runtime.getRuntime().exec(fullCommand)
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                println("ADB Error: $error")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
