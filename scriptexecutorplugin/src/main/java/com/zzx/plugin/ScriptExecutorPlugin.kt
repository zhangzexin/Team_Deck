package com.zzx.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.zzx.common.plugin.IPlugin

/**
 * 远程脚本执行插件
 * 支持在电脑端配置脚本路径，并通过手机端远程触发执行
 */
class ScriptExecutorPlugin : IPlugin {
    override val id: String = "script_executor_plugin"
    override val name: String = "脚本执行器"

    override var messageSender: ((String, String) -> Unit)? = null
    private val gson = Gson()

    // 辅助判断是否为桌面端 (用于隔离 AWT/Executor 逻辑)
    private fun isDesktop(): Boolean {
        return try {
            Class.forName("java.util.prefs.Preferences")
            true
        } catch (e: Exception) {
            false
        }
    }

    // 获取持久化保存的脚本路径 (仅限桌面端)
    private fun getPersistedPath(): String {
        return try {
            val prefs = java.util.prefs.Preferences.userNodeForPackage(ScriptExecutorPlugin::class.java)
            prefs.get("script_path", "")
        } catch (e: Exception) {
            ""
        }
    }

    // 保存脚本路径 (仅限桌面端)
    private fun setPersistedPath(path: String) {
        try {
            val prefs = java.util.prefs.Preferences.userNodeForPackage(ScriptExecutorPlugin::class.java)
            prefs.put("script_path", path)
            prefs.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        if (actionId == "click") {
            // 只有在手机端点击时才发送指令
            if (!isDesktop()) {
                println("Script Plugin: Mobile click detected, sending run_script command.")
                
                val envelope = mapOf(
                    "code" to 10,
                    "msg" to "trigger",
                    "data" to mapOf(
                        "pluginId" to id,
                        "data" to "run_script"
                    )
                )
                messageSender?.invoke(id, gson.toJson(envelope))
            }
        }
    }

    override fun onReceive(data: String) {
        // 只有在桌面端接收到特定指令时才执行逻辑
        if (isDesktop() && data == "run_script") {
            println("Script Plugin: Desktop received run_script command, triggering local process.")
            executeLocalScript()
        }
    }

    private fun executeLocalScript() {
        val path = getPersistedPath()
        if (path.isBlank()) {
            println("Script Plugin Error: Script path is empty. Please configure in Settings.")
            return
        }

        try {
            val file = java.io.File(path)
            if (!file.exists()) {
                println("Script Plugin Error: File not found at $path")
                return
            }

            println("Script Plugin: Starting process: $path")
            val pb = ProcessBuilder(path)
            // 设置工作目录为脚本所在目录，方便相对路径引用
            pb.directory(file.parentFile)
            // 合并错误流
            pb.redirectErrorStream(true)
            pb.start()
            println("Script Plugin: Process started successfully.")
        } catch (e: Exception) {
            println("Script Plugin Error: Failed to start process: ${e.message}")
            e.printStackTrace()
        }
    }

    @Composable
    override fun AppUI() {
        // 手机端展示：紫色渐变 + 终端图标
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF673AB7), Color(0xFF311B92))
                    )
                )
                .clickable { onTrigger("click") },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Run Script",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
                Text("执行脚本", color = Color.White, fontSize = 12.sp)
            }
        }
    }

    @Composable
    override fun DesktopUI() {
        // 桌面端列表格预览
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("脚本执行器", fontSize = 12.sp)
            val path = getPersistedPath()
            Text(
                if (path.isEmpty()) "未配置路径" else "已就绪",
                fontSize = 10.sp,
                color = if (path.isEmpty()) Color.Red else Color.Gray
            )
        }
    }

    @Composable
    override fun SettingsUI() {
        // 桌面端专属配置页面
        var scriptPath by remember { mutableStateOf(getPersistedPath()) }
        var statusMsg by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("核心配置", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = scriptPath,
                onValueChange = { scriptPath = it },
                label = { Text("本地脚本绝对路径") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如 D:\\tools\\start.bat 或 /usr/bin/myscript.sh") }
            )

            Button(
                onClick = {
                    setPersistedPath(scriptPath)
                    statusMsg = "配置已保存成功"
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("保存设置")
            }

            if (statusMsg.isNotEmpty()) {
                Text(statusMsg, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()

            Text("使用帮助：", style = MaterialTheme.typography.labelLarge)
            Text("1. 请输入能够直接双击运行或由系统执行的完整路径。", fontSize = 12.sp, color = Color.Gray)
            Text("2. 对于 Python 脚本，建议使用绝对路径的解释器或将其包装在 .bat/.sh 中。", fontSize = 12.sp, color = Color.Gray)
            Text("3. 路径保存后，手机端的点击指令将实时触发此处的本地进程。", fontSize = 12.sp, color = Color.Gray)
        }
    }
    override fun onDestroy() {
        println("ScriptExecutorPlugin: Cleanup called.")
    }
}
