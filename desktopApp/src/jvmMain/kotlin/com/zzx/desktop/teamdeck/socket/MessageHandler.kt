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

                is InitUiMessageType -> {
                    val event = msgAdapter.buildMessage(gson, msg) as Message<InitUiEvent>
                    FlowBus.with<Message<InitUiEvent>>(msgAdapter.code.name)
                        .post(coroutineScope, event)
                    
                    // 【关键触发】接收到手机端 UI 状态和清单后，由桌面端按需开启同步任务
                    com.zzx.desktop.teamdeck.utils.NsdManagerUtils.Instance.syncMissingPlugins(event.data.currentPluginIds)
                }

                is ErrorMessageType -> FlowBus.with<SimpleMessage>(msgAdapter.code.name)
                    .post(coroutineScope, msgAdapter.buildMessage(gson, msg))

                is ItemConfigMessageType -> FlowBus.with<Message<ItemConfigEvent>>(msgAdapter.code.name)
                    .post(coroutineScope, msgAdapter.buildMessage(gson, msg))

                is PluginCustomMessageType -> {
                    val pluginMsg = msgAdapter.buildMessage(gson, msg) as Message<PluginCustomEvent>
                    com.zzx.common.plugin.PluginManager.dispatchPluginMessage(pluginMsg.data.pluginId, pluginMsg.data.data)
                }

                is PluginUninstallMessageType -> {
                    val uninstallMsg = msgAdapter.buildMessage(gson, msg) as Message<PluginUninstallEvent>
                    com.zzx.common.plugin.PluginManager.removePlugin(uninstallMsg.data.pluginId, notifyRemote = false)
                }

                null -> {}
            }
        } catch (e: Exception) {

        }
    }

    private fun buildMessageAdapter(simpleMessage: SimpleMessage): BaseMessageType<*>? {
        return when (simpleMessage.code) {
            CodeEnum.ERROR.value -> ErrorMessageType
            CodeEnum.INIT.value -> InitMessageType
            CodeEnum.INITUI.value -> InitUiMessageType
            CodeEnum.ITEMCONFIG.value -> ItemConfigMessageType
            CodeEnum.PLUGIN_CUSTOM.value -> PluginCustomMessageType
            CodeEnum.PLUGIN_UNINSTALL.value -> PluginUninstallMessageType
            else -> null
        }
    }


}
