package com.zzx.desktop.teamdeck.ext

import com.zzx.common.ext.fromJson
import com.zzx.desktop.teamdeck.flowBus.FlowBus
import kotlinx.coroutines.CoroutineScope


inline fun <reified T> Any.postBus(key:String,scope: CoroutineScope) {
    FlowBus.with<T>(key).post(scope,this as T)
}

inline fun <reified T> String.toBeanAndPostBus(key:String,scope: CoroutineScope) {
    val fromJson = this.fromJson<T>()
    FlowBus.with<T>(key).post(scope,fromJson)
}