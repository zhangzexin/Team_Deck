package com.zzx.common.plugin

import androidx.compose.runtime.Composable

/**
 * Team Deck 插件基础接口
 */
interface IPlugin {
    /** 插件唯一 ID */
    val id: String
    
    /** 插件显示名称 */
    val name: String

    /**
     * Android 手机端展示的 UI
     */
    @Composable
    fun AppUI()

    /**
     * Desktop 电脑端展示的 UI (作为网格中的卡片)
     */
    @Composable
    fun DesktopUI()

    /**
     * Desktop 电脑端展示的详尽设置页面 (点击卡片后进入)
     */
    @Composable
    fun SettingsUI() {}

    /**
     * 业务逻辑触发回调
     * @param actionId 动作标识
     * @param params 传递参数 (JSON 或 键值对)
     */
    fun onTrigger(actionId: String, params: Map<String, String> = emptyMap())

    /**
     * 接收自定义插件数据 (用于解耦宿主与具体的业务)
     */
    fun onReceive(data: String) {}

    /**
     * 持有的消息发送接口，由宿主在加载时注入
     */
    var messageSender: ((pluginId: String, data: String) -> Unit)?
}

/**
 * 判断插件是否实现了设置页面 (Desktop 专用检查)
 */
expect fun IPlugin.isSettingsSupported(): Boolean
