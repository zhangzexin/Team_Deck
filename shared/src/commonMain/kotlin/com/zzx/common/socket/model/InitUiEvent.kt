package com.zzx.common.socket.model

/**
 * @描述：初始化消息，告知PC端当前手机状态及已安装插件
 * @param number 显示总个数
 * @param columns 显示几列
 * @param currentPluginIds 手机端当前已加载的插件 ID 列表，用于增量同步
 */
data class InitUiEvent(
    val number: Int, 
    val columns: Int,
    val currentPluginIds: List<String> = emptyList()
)