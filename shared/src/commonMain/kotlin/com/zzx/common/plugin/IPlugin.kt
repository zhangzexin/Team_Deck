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
     * Desktop 电脑端展示的 UI
     */
    @Composable
    fun DesktopUI()

    /**
     * 业务逻辑触发回调
     * @param actionId 动作标识
     * @param params 传递参数 (JSON 或 键值对)
     */
    fun onTrigger(actionId: String, params: Map<String, String> = emptyMap())
}
