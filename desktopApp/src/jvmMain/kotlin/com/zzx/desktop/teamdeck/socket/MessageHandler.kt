package com.zzx.desktop.teamdeck.socket

import com.google.gson.Gson
import com.zzx.common.socket.model.SimpleMessage
import com.zzx.common.socket.model.*
import com.zzx.common.socket.type.*
import com.zzx.desktop.teamdeck.flowBus.FlowBus
import kotlinx.coroutines.CoroutineScope

object MessageHandler {
    val gson by lazy {
        Gson()
    }

    fun dispatchMsg(coroutineScope: CoroutineScope, msg: String) {
        try {
            val simpleMessage = gson.fromJson(msg, SimpleMessage::class.java)
//            when (simpleMessage.code) {
//                CodeEnum.INIT.value -> {
//
//                    msg.toBeanAndPostBus<Message<InitEvent>>(CodeEnum.INIT.name,coroutineScope)
//                }
//
//                CodeEnum.INITUI.value -> {
//                    msg.toBeanAndPostBus<Message<InitUiEvent>>(CodeEnum.INITUI.name,coroutineScope)
//                }
//
//                CodeEnum.ITEMCONFIG.value -> {
//                    msg.toBeanAndPostBus<Message<ItemConfigEvent>>(CodeEnum.ITEMCONFIG.name,coroutineScope)
//                }
//
//                CodeEnum.ERROR.value -> {
//                    msg.toBeanAndPostBus<SimpleMessage>(CodeEnum.ERROR.name,coroutineScope)
//                }
//            }
            val msgAdapter = buildMessageAdapter(simpleMessage)
            when(msgAdapter) {
                is InitMessageType -> FlowBus.with<Message<InitEvent>>(msgAdapter.code.name)
                    .post(coroutineScope, msgAdapter.buildMessage(gson, msg))

                is InitUiMessageType -> FlowBus.with<Message<InitUiEvent>>(msgAdapter.code.name)
                    .post(coroutineScope, msgAdapter.buildMessage(gson, msg))

                is ErrorMessageType -> FlowBus.with<SimpleMessage>(msgAdapter.code.name)
                    .post(coroutineScope, msgAdapter.buildMessage(gson, msg))

                is ItemConfigMessageType -> FlowBus.with<Message<ItemConfigEvent>>(msgAdapter.code.name)
                    .post(coroutineScope, msgAdapter.buildMessage(gson, msg))

                null -> {}
            }
        } catch (e: Exception) {

        }
    }

    private fun buildMessageAdapter(simpleMessage: SimpleMessage): BaseMessageType<*>? {
        return when (simpleMessage.code) {
            CodeEnum.ERROR.ordinal -> ErrorMessageType
            CodeEnum.INIT.ordinal -> InitMessageType
            CodeEnum.INITUI.ordinal -> InitUiMessageType
            CodeEnum.ITEMCONFIG.ordinal -> ItemConfigMessageType
            else -> null
        }
    }


}
