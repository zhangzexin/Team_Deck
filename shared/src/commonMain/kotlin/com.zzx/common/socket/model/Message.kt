package com.zzx.common.socket.model


/**
 *@描述：websocke消息基类
 *@time：2023/9/15
 *@author:zhangzexin
 * @param code 消息类型
 * @param data 具体消息
 * @param msg String类型，用于消息提示信息
 */
class Message<T>(code: Int, msg: String, val data: T) : SimpleMessage(code, msg)
