package com.zzx.common.socket.model

/**
 *@描述：初始化消息，告知PC端当前手机能展示个数
 *@time：2023/9/15
 *@author:zhangzexin
 * @param number 显示总个数
 * @param columns 显示几列
 */
data class InitUiEvent(val number: Int, val columns: Int)