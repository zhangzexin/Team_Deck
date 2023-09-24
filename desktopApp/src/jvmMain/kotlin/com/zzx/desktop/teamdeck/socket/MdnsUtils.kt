package com.zzx.desktop.teamdeck.socket

import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import javax.jmdns.ServiceTypeListener

class MdnsUtils {
    var jmdns: JmDNS?

    constructor() {
        val address = InetAddress.getLocalHost()
        val hostname = address.hostName
        jmdns = JmDNS.create(address, hostname)
    }

    companion object {
        val instance by lazy(LazyThreadSafetyMode.NONE) {
            MdnsUtils()
        }
    }

    fun setUp(port: Int) {
        val serviceType = "_http._tcp."
        val serverName = "Team Deck Server"
        val serviceInfo = ServiceInfo.create(serviceType, serverName, port,"活动板甲控制器")
        //注册服务
        jmdns?.registerService(serviceInfo)
        jmdns?.addServiceTypeListener(object : ServiceTypeListener{
            override fun serviceTypeAdded(event: ServiceEvent?) {
                println("serviceTypeAdded:"+event?.info)
            }

            override fun subTypeForServiceTypeAdded(event: ServiceEvent?) {
                println("subTypeForServiceTypeAdded:"+event?.info)
            }

        })
        jmdns?.addServiceListener("_http._tcp.local.", object : ServiceListener{
            override fun serviceAdded(event: ServiceEvent?) {
                // 请求服务信息。
//                jmdns?.requestServiceInfo(event?.type, event?.name, 1000)
                println("serviceAdded:"+event?.info)
            }

            override fun serviceRemoved(event: ServiceEvent?) {
                println("serviceRemoved:"+event?.info)
            }

            override fun serviceResolved(event: ServiceEvent?) {
                // 在这里是检索到了服务，但是为啥是这个命名。然后再这里就获取信息就自己处理逻辑，比较服务很多。
                println("serviceResolved:"+event?.info)
            }

        })
    }

    fun stop() {
        jmdns?.unregisterAllServices()
    }


}