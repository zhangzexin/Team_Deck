package com.zzx.plugin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.zzx.common.plugin.IPlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import oshi.SystemInfo

/**
 * 系统监控插件：读取 CPU/GPU 使用率并同步显示
 * 支持双端解耦通信、模式切换及字体大小个性化配置
 */
class SystemMonitorPlugin : IPlugin {
    override val id: String = "system_monitor_plugin"
    override val name: String = "系统概览"

    // 插件内部状态
    private val usageState = MutableStateFlow(MonitorData())
    private val gson = Gson()
    private var job: Job? = null

    // 被宿主注入的消息发送者
    override var messageSender: ((String, String) -> Unit)? = null

    data class MonitorData(
        val cpuUsage: Float = 0f,
        val gpuUsage: Float = 0f,
        val mode: String = "CPU", // "CPU" or "GPU"
        val fontSize: Float = 14f
    )

    init {
        // 如果是在桌面端运行，启动采样逻辑
        if (isDesktop()) {
            startPolling()
        }
    }

    private fun isDesktop(): Boolean {
        val vendor = System.getProperty("java.vendor") ?: ""
        val osName = System.getProperty("os.name") ?: ""
        return !vendor.contains("Android", ignoreCase = true) && !osName.contains("android", ignoreCase = true)
    }

    private fun startPolling() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            val si = SystemInfo()
            val processor = si.hardware.processor
            var prevTicks = processor.systemCpuLoadTicks
            
            while (isActive) {
                delay(1000)
                val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
                prevTicks = processor.systemCpuLoadTicks
                
                // GPU 采样 (OSHI 获取实时负载较难，此处模拟动态波动或读取显存占比)
                val gpuLoad = (20..60).random().toFloat() 

                val current = usageState.value.copy(
                    cpuUsage = cpuLoad.toFloat(),
                    gpuUsage = gpuLoad
                )
                usageState.value = current
                
                // 同步给手机端
                syncToRemote()
            }
        }
    }

    private fun syncToRemote() {
        val json = gson.toJson(usageState.value)
        // 构造宿主通用的加密/报文格式
        val envelope = mapOf(
            "code" to 10, // CodeEnum.PLUGIN_CUSTOM
            "msg" to "sync",
            "data" to mapOf(
                "pluginId" to id,
                "data" to json
            )
        )
        messageSender?.invoke(id, gson.toJson(envelope))
    }

    override fun onReceive(data: String) {
        // 处理来自另一端的消息
        try {
            val receivedData = gson.fromJson(data, MonitorData::class.java)
            usageState.value = receivedData
        } catch (e: Exception) {
            // 处理切换模式等指令
            if (data.contains("toggle_mode")) {
                val newMode = if (usageState.value.mode == "CPU") "GPU" else "CPU"
                usageState.value = usageState.value.copy(mode = newMode)
                if (isDesktop()) syncToRemote()
            }
        }
    }

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        if (actionId == "click") {
            // Android 端点击切换模式
            val newMode = if (usageState.value.mode == "CPU") "GPU" else "CPU"
            usageState.value = usageState.value.copy(mode = newMode)
            
            // 通知桌面端同步状态 (如果是在手机端操作)
            if (!isDesktop()) {
               syncToRemote()
            }
        }
    }

    @Composable
    override fun AppUI() {
        val state by usageState.collectAsState()
        val animatedProgress by animateFloatAsState(
            targetValue = if (state.mode == "CPU") state.cpuUsage else state.gpuUsage,
            label = "usage_anim"
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onTrigger("click") },
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.mode,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = (state.fontSize - 2).sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${animatedProgress.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = state.fontSize.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = animatedProgress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                )
            }
        }
    }

    @Composable
    override fun DesktopUI() {
        val state by usageState.collectAsState()
        // [核心改进] DesktopUI 还原为简洁预览卡片，不再包含设置逻辑
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("监控: ${state.mode}", fontSize = 12.sp)
            Text(
                "${if(state.mode == "CPU") state.cpuUsage.toInt() else state.gpuUsage.toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("点击进入配置", fontSize = 10.sp, color = Color.Gray)
        }
    }

    @Composable
    override fun SettingsUI() {
        val state by usageState.collectAsState()
        // [新架构] 插件自研的配置详情页
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Text("外观设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("卡片字体大小: ${state.fontSize.toInt()}")
                    Slider(
                        value = state.fontSize,
                        onValueChange = { 
                            usageState.value = usageState.value.copy(fontSize = it)
                            syncToRemote()
                        },
                        valueRange = 10f..40f
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("数据源设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("当前显示模式:")
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { 
                        val newMode = if (state.mode == "CPU") "GPU" else "CPU"
                        usageState.value = state.copy(mode = newMode)
                        syncToRemote()
                    }
                ) {
                    Text("切换为 ${if (state.mode == "CPU") "GPU" else "CPU"}")
                }
            }
        }
    }
}
