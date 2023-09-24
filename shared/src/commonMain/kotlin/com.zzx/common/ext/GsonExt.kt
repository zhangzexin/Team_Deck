package com.zzx.common.ext

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 *@描述：Gson扩展类
 *@time：2023/9/17
 *@author:zhangzexin
 */
object GsonExt {
    val gson: Gson by lazy {
        Gson()
    }
}


fun Any.toJson(includeNulls: Boolean = true): String{
    return GsonExt.gson.toJson(includeNulls)
}

inline fun <reified T> String.fromJson():T {
    val type = object : TypeToken<T>(){}.type
    return GsonExt.gson.fromJson(this, type)
}

fun <T> String.fromJson(clazz: Class<T>): T {
    return GsonExt.gson.fromJson(this, clazz)
}
