package com.zzx.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.zzx.common.plugin.IPlugin

/**
 * Android Studio 远程运行插件
 * 手机端点击 -> 发送指令 -> 电脑端模拟 Shift + F10
 */
class AndroidStudioPlugin : IPlugin {
    override val id: String = "android_studio_plugin"
    override val name: String = "AS 运行"

    override var messageSender: ((String, String) -> Unit)? = null
    private val gson = Gson()

    // 键盘常量 (来自 java.awt.event.KeyEvent)
    private val VK_SHIFT = 16
    private val VK_F10 = 121

    // 辅助判断是否为桌面端 (避免在 Android 端误调 AWT API)
    private fun isDesktop(): Boolean {
        return try {
            Class.forName("java.awt.Robot")
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        if (actionId == "click") {
            // 只有在手机端点击时才发送指令
            if (!isDesktop()) {
                println("AS Plugin: Mobile click detected, sending run_as command via envelope.")
                
                // 核心修复：必须封装成宿主系统识别的 JSON 报文格式 (PLUGIN_CUSTOM = 10)
                val envelope = mapOf(
                    "code" to 10,
                    "msg" to "trigger",
                    "data" to mapOf(
                        "pluginId" to id,
                        "data" to "run_as"
                    )
                )
                messageSender?.invoke(id, gson.toJson(envelope))
            }
        }
    }

    override fun onReceive(data: String) {
        // 只有在桌面端接收到特定指令时才执行模拟按键
        if (isDesktop() && data == "run_as") {
            println("AS Plugin: Desktop received run_as command, triggering Robot via reflection.")
            triggerAndroidStudioRun()
        }
    }

    private fun triggerAndroidStudioRun() {
        try {
            // 使用反射调用 AWT Robot，避免 Android 端编译失败
            val robotClass = Class.forName("java.awt.Robot")
            val robot = robotClass.getDeclaredConstructor().newInstance()
            
            val keyPressMethod = robotClass.getMethod("keyPress", Int::class.javaPrimitiveType)
            val keyReleaseMethod = robotClass.getMethod("keyRelease", Int::class.javaPrimitiveType)

            // Android Studio (Windows) 默认运行快捷键是 Shift + F10
            keyPressMethod.invoke(robot, VK_SHIFT)
            keyPressMethod.invoke(robot, VK_F10)
            keyReleaseMethod.invoke(robot, VK_F10)
            keyReleaseMethod.invoke(robot, VK_SHIFT)
            
            println("AS Plugin: Robot (Reflection) simulated Shift + F10 successfully.")
        } catch (e: Exception) {
            println("AS Plugin Error: Failed to simulate keypress via reflection: ${e.message}")
            e.printStackTrace()
        }
    }

    @Composable
    override fun AppUI() {
        // 手机端展示：安卓绿渐变背景 + 播放图标
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF3DDC84), Color(0xFF073042))
                    )
                )
                .clickable { onTrigger("click") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Run",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
    }

    @Composable
    override fun DesktopUI() {
        // 桌面端展示：圆形图标预览
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFF3DDC84)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Android Studio", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text("远程运行", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    override fun onDestroy() {
        println("AndroidStudioPlugin: Cleanup called.")
    }
}
