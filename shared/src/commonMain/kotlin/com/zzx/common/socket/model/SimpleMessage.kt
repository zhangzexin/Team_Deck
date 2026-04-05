package com.zzx.common.socket.model


/**
 *@描述：简单形Message只需要识别code，然后再对data解析
 *@time：2023/9/16
 *@author:zhangzexin
 */
open class SimpleMessage(open val code : Int, open val msg:String)