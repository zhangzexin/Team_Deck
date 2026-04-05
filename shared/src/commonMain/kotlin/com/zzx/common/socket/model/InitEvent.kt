package com.zzx.common.socket.model

/**
 *@描述：初始化事件，一般由desktop端传入
 *@time：2023/9/16
 *@author:zhangzexin
 * @param configEvent 提供插件信息
 */
data class InitEvent(val configEvent: List<ItemConfigEvent>)
