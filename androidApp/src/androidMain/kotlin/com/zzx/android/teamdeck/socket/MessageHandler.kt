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

                is PluginCustomMessageType -> {
                    val pluginMsg = msgAdapter.buildMessage(gson, msg) as Message<PluginCustomEvent>
                    com.zzx.common.plugin.PluginManager.dispatchPluginMessage(pluginMsg.data.pluginId, pluginMsg.data.data)
                }

                is PluginUninstallMessageType -> {
                    val uninstallMsg = msgAdapter.buildMessage(gson, msg) as Message<PluginUninstallEvent>
                    com.zzx.common.plugin.PluginManager.removePlugin(uninstallMsg.data.pluginId, notifyRemote = false)
                }
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
            CodeEnum.PLUGIN_CUSTOM.value -> PluginCustomMessageType
            CodeEnum.PLUGIN_UNINSTALL.value -> PluginUninstallMessageType
            else -> null
        }
    }
}