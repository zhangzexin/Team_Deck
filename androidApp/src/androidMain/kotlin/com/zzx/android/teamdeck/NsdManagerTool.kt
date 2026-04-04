package com.zzx.android.teamdeck

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.annotation.RequiresApi


class NsdManagerTool internal constructor(
    context: Context,
) {
    private var mDiscoveryListener: DiscoveryListener? = null
    var onInfoCallBack: InfoCallBack? = null
    var desList: MutableList<NsdServiceInfo> = ArrayList()
    var tempServiceList: MutableList<NsdServiceInfo> = ArrayList()

    val mResolveListener: NsdManager.ResolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, i: Int) {
            Log.d("xm--------", "onResolveFailed")
        }

        override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
            Log.d("TeamDeck-Net", "Resolve Success: ${nsdServiceInfo.serviceName} at ${nsdServiceInfo.host}:${nsdServiceInfo.port}")
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
    }

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
                    Log.d("TeamDeck-Net", "Discovery Started: $s")
                }

                override fun onDiscoveryStopped(s: String) {
                    Log.d("xm--------", "onDiscoveryStopped")
                }

                override fun onServiceFound(nsdServiceInfo: NsdServiceInfo) {
                    Log.d("xm--------", "onServiceFound: ${nsdServiceInfo.serviceName}")
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
                        Log.d("TeamDeck-Net", "New Service Found, resolving: ${nsdServiceInfo.serviceName}")
                        // 【优化点】只解析最新发现的服务，不再循环遍历全列表解析
                        nsdManager?.resolveService(nsdServiceInfo, mResolveListener)
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