package com.zzx.android.teamdeck.socket

import com.google.gson.Gson
import com.zzx.common.socket.model.*
import com.zzx.common.socket.type.*
import com.zzx.android.teamdeck.flowBus.FlowBus
import com.zzx.common.socket.type.CodeEnum.ERROR
import com.zzx.common.socket.type.CodeEnum.INIT
import com.zzx.common.socket.type.CodeEnum.INITUI
import com.zzx.common.socket.type.CodeEnum.ITEMCONFIG


/**
 *@描述：消息处理接口
 *@time：2023/9/16
 *@author:zhangzexin
 */
object MessageHandler {
    val gson by lazy {
        Gson()
    }

    fun dispatchersMsg(msg: String) {
        try {
            val simpleMessage = gson.fromJson(msg, SimpleMessage::class.java)
            when (val msgAdapter = buildMessageAdapter(simpleMessage)) {
                is InitMessageType -> FlowBus.with<Message<InitEvent>>(msgAdapter.code.name)
                    .post(null, msgAdapter.buildMessage(gson, msg))

                is InitUiMessageType -> FlowBus.with<Message<InitUiEvent>>(msgAdapter.code.name)
                    .post(null, msgAdapter.buildMessage(gson, msg))

                is ErrorMessageType -> FlowBus.with<SimpleMessage>(msgAdapter.code.name)
                    .post(null, msgAdapter.buildMessage(gson, msg))

                is ItemConfigMessageType -> FlowBus.with<Message<ItemConfigEvent>>(msgAdapter.code.name)
                    .post(null, msgAdapter.buildMessage(gson, msg))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun buildMessageAdapter(simpleMessage: SimpleMessage): BaseMessageType<*>? {
        return when (simpleMessage.code) {
            ERROR.ordinal -> ErrorMessageType
            INIT.ordinal -> InitMessageType
            INITUI.ordinal -> InitUiMessageType
            ITEMCONFIG.ordinal -> ItemConfigMessageType
            else -> null
        }
    }
}