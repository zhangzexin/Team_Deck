package com.zzx.common.socket.type

import com.zzx.common.socket.model.InitEvent
import com.zzx.common.socket.model.InitUiEvent
import com.zzx.common.socket.model.ItemConfigEvent
import com.zzx.common.socket.model.Message
import com.zzx.common.socket.model.PluginCustomEvent
import com.zzx.common.socket.model.PluginUninstallEvent
import com.zzx.common.socket.model.SimpleMessage

/**
 *@描述：所有消息类型
 *@time：2023/9/16
 *@author:zhangzexin
 */
//object InitMessageType : BaseMessageType<Message<InitEvent>>(CodeEnum.INIT)
//
//object InitUiMessageType : BaseMessageType<Message<InitUiEvent>>(CodeEnum.INITUI)
//
//object ItemConfigMessageType : BaseMessageType<Message<ItemConfigEvent>>(CodeEnum.ITEMCONFIG)
//
//object ErrorMessageType  : BaseMessageType<SimpleMessage>(CodeEnum.ITEMCONFIG)

data object InitMessageType : BaseMessageType<Message<InitEvent>>(CodeEnum.INIT)
data object InitUiMessageType : BaseMessageType<Message<InitUiEvent>>(CodeEnum.INITUI)
data object ItemConfigMessageType : BaseMessageType<Message<ItemConfigEvent>>(CodeEnum.ITEMCONFIG)
data object ErrorMessageType : BaseMessageType<SimpleMessage>(CodeEnum.ERROR)
data object PluginCustomMessageType : BaseMessageType<Message<PluginCustomEvent>>(CodeEnum.PLUGIN_CUSTOM)
data object PluginUninstallMessageType : BaseMessageType<Message<PluginUninstallEvent>>(CodeEnum.PLUGIN_UNINSTALL)

//object MessageType {
//    val initMessageType = messageTypeOf<Message<InitEvent>>(CodeEnum.INIT)
//    val initUiMessageType = messageTypeOf<Message<InitUiEvent>>(CodeEnum.INITUI)
//    val itemConfigMessageType = messageTypeOf<Message<ItemConfigEvent>>(CodeEnum.ITEMCONFIG)
//    val errorMessageType = messageTypeOf<SimpleMessage>(CodeEnum.ERROR)
//}