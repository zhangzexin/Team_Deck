package com.zzx.android.teamdeck

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.util.Log


class NsdManagerTool internal constructor(
    context: Context,
) {
    private var mDiscoveryListener: DiscoveryListener? = null
    var onInfoCallBack: InfoCallBack? = null
    var desList: MutableList<NsdServiceInfo> = ArrayList()
    var tempServiceList: MutableList<NsdServiceInfo> = ArrayList()

    init {
        if (nsdManager == null) {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        }
    }

    fun start() {
        desList.clear()
        Log.d("xm--------", "start")
        tempServiceList.clear()
        createDiscoverListener()
    }

    fun stop() {
        mDiscoveryListener?.let { nsdManager?.stopServiceDiscovery(mDiscoveryListener) }
        mDiscoveryListener = null
        Log.d("xm--------", "stop")
        desList.clear()
    }

    private fun createDiscoverListener() {
        if (mDiscoveryListener == null) {
            mDiscoveryListener = object : DiscoveryListener {
                //                private NsdServiceInfo mmNsdServiceInfo;
                override fun onStartDiscoveryFailed(s: String, i: Int) {
                    Log.d("xm--------", "onStartDiscoveryFailed")
                }

                override fun onStopDiscoveryFailed(s: String, i: Int) {
                    Log.d("xm--------", "onStopDiscoveryFailed")
                }

                override fun onDiscoveryStarted(s: String) {
                    Log.d("xm--------", "onDiscoveryStarted")
                }

                override fun onDiscoveryStopped(s: String) {
                    Log.d("xm--------", "onDiscoveryStopped")
                }

                override fun onServiceFound(nsdServiceInfo: NsdServiceInfo) {
//                    mmNsdServiceInfo = nsdServiceInfo;
                    //这里的nsdServiceInfo只能获取到名字,ip和端口都不能获取到,要想获取到需要调用NsdManager.resolveService方法
                    Log.d("xm--------", "onServiceFound")
                    Log.d("xm--------", nsdServiceInfo.serviceName)
                    var didHad = false
                    for (i in tempServiceList.indices) {
                        val currentInfo = tempServiceList[i]
                        if (nsdServiceInfo.serviceName == currentInfo.serviceName) {
                            didHad = true
                            break
                        }
                    }
                    if (!didHad) {
                        tempServiceList.add(nsdServiceInfo)
                    }
                    for (info in tempServiceList) {
                        nsdManager!!.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, i: Int) {
                                Log.d("xm--------", "onResolveFailed")
                            }

                            override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
                                Log.d("xm--------", "onServiceResolved")
                                var didHad = false
                                for (i in desList.indices) {
                                    val info = desList[i]
                                    if (info.serviceName == nsdServiceInfo.serviceName) {
                                        didHad = true
                                        break
                                    }
                                }
                                if (!didHad) {
                                    desList.add(nsdServiceInfo)
                                }
                                onInfoCallBack?.msgCallback(desList)
                            }
                        })
                        try {
                            Thread.sleep(10)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onServiceLost(nsdServiceInfo: NsdServiceInfo) {
                    Log.d("xm--------", "onServiceLost")
                }
            }
        }
        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
    }

    companion object {
        private var nsdManager: NsdManager? = null
        private const val SERVICE_TYPE = "_http._tcp."
    }

    fun dispose() {
        nsdManager = null
        onInfoCallBack = null
    }

    fun setInfoCallBack(onInfoCallBack: InfoCallBack) {
        this.onInfoCallBack = onInfoCallBack
    }
}

interface InfoCallBack {
    fun msgCallback(list: List<NsdServiceInfo>)
}