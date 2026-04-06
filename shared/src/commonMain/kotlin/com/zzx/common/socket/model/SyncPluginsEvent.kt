package com.zzx.common.socket.model

/**
 * 插件同步事件：桌面端发送给手机端当前已安装的插件 ID 列表
 */
data class SyncPluginsEvent(
    val installedIds: List<String>
)
