package com.zzx.common.socket.type

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


/**
 *@描述：
 *@time：2023/9/16
 *@author:zhangzexin
 */
abstract class BaseMessageType<in T>(val code: CodeEnum) {
    inline fun <reified T> buildMessage(gson: Gson, msg:String): T {
        val type = object : TypeToken<T>(){}.type
        return gson.fromJson(msg,type)
    }
}
