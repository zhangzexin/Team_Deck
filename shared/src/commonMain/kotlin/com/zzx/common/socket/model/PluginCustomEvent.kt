package com.zzx.common.socket.model

/**
 * 通用插件自定义事件 (用于解耦宿主与插件业务)
 */
data class PluginCustomEvent(
    val pluginId: String,
    val data: String
)
