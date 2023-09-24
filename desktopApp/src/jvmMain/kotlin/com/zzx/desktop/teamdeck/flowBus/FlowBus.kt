package com.zzx.desktop.teamdeck.flowBus

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.CoroutineContext

/**
 *@描述：用flow实现的EventBus
 *@time：2023/9/16
 *@author:zhangzexin
 */

object FlowBus {
    private val busMap = mutableMapOf<String, EventBus<*>>()
    private val busStickMap = mutableMapOf<String, StickEventBus<*>>()

    @Synchronized
    fun <T> with(key: String): EventBus<T> {
        var eventBus = busMap[key]
        if (eventBus == null) {
            eventBus = EventBus<T>(key)
            busMap[key] = eventBus
        }
        return eventBus as EventBus<T>
    }

    @Synchronized
    fun <T> withStick(key: String): StickEventBus<T> {
        var eventBus = busStickMap[key]
        if (eventBus == null) {
            eventBus = StickEventBus<T>(key)
            busStickMap[key] = eventBus
        }
        return eventBus as StickEventBus<T>
    }

    //真正实现类
    open class EventBus<T>(private val key: String, private var count: Int = 0) : CoroutineScope {

        //私有对象用于发送消息
        private val _events: MutableSharedFlow<T> by lazy {
            obtainEvent()
        }

        //暴露的公有对象用于接收消息
        val events = _events.asSharedFlow()

        protected open fun obtainEvent(): MutableSharedFlow<T> = MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)

        //主线程接收数据
        fun register(scope: CoroutineScope, action: (t: T) -> Unit) {
            scope.launch {
                events.collect {
                    try {
                        action(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        //协程中发送数据
        suspend fun post(event: T) {
            _events.emit(event)
        }

        //主线程发送数据
        fun post(scope: CoroutineScope, event: T) {
            scope.launch {
                _events.emit(event)
            }
        }

        //手动销毁
        fun unRegister() {
            val subscriptCount = _events.subscriptionCount.value
            if (subscriptCount <= 0)
                busMap.remove(key)
        }

        override val coroutineContext: CoroutineContext
            get() = TODO("Not yet implemented")
    }

    class StickEventBus<T>(key: String) : EventBus<T>(key) {
        override fun obtainEvent(): MutableSharedFlow<T> = MutableSharedFlow(1, 1, BufferOverflow.DROP_OLDEST)
    }

}
