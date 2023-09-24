package com.zzx.common.socket.model

/**
 *@描述：加载插件的配置
 *@time：2023/9/15
 *@author:zhangzexin
 * @param backgroundImage 用于显示item中的背景图片
 * @param libPath 插件库路径
 * @param mainclass 插件UI起始路径
 */
data class ItemConfigEvent(val libPath:String, val mainclass:String)
