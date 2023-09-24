# 使用android自带的mDNS
## 目标
用于查找同一个局域网环境下的设备
## 官方说明([链接]("https://developer.android.com/training/connect-devices-wirelessly/nsd?hl=zh-cn"))：

网络服务发现 (NSD) 可让您的应用访问其他设备在本地网络上提供的服务。支持 NSD 的设备包括打印机、网络摄像头、HTTPS 服务器以及其他移动设备。

NSD 实现了基于 DNS 的服务发现 (DNS-SD) 机制，该机制允许您的应用通过指定服务类型和提供所需类型服务的设备实例的名称来请求服务。Android 和其他移动平台均支持 DNS-SD。

将 NSD 添加到应用中，可让您的用户识别本地网络上是否有其他设备支持您的应用所请求的服务。这对于各种点对点应用非常有用，例如文件共享或多人游戏。Android 的 NSD API 简化了实现此类功能所需的工作。
## 注意事项

调用registerService/discoverServices时，传入serviceType一定要注意规则，不然一直会注册失败，回调onStartDiscoveryFailed(),error_code = 0

示例代码
```
//初始化官方的NSD服务
    val nsdManger by lazy {
        getSystemService(NSD_SERVICE) as NsdManager
    }

    val address by lazy {
        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        wifiManager?.connectionInfo?.let {
            InetAddress.getByName(
                String.format(
                    Locale.ENGLISH,
                    "%d.%d.%d.%d",
                    it.ipAddress and 0xff,
                    it.ipAddress shr 8 and 0xff,
                    it.ipAddress shr 16 and 0xff,
                    it.ipAddress shr 24 and 0xff
                )
            )
        }
    }
    val mLocalPort by lazy {
        ServerSocket(0).let { socket ->
            // Store the chosen port.
            socket.localPort
        }
    }
    private var mServiceName: String? = "iphonefsd4a5f6dsa789"
    val SERVICE_TYPE = "_http._tcp."
    fun registerService() {
            // Create the NsdServiceInfo object, and populate it.
            val serviceInfo = NsdServiceInfo().apply {
                // The name is subject to change based on conflicts
                // with other services advertised on the same network.
                serviceName = mServiceName
                serviceType = SERVICE_TYPE
                host = address
                port = mLocalPort
            }
            
            //注册加入服务，使其能被扫描
            nsdManger.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            //开启扫描服务，返回扫描结果
            nsdManger.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
    
    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.serviceName
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // 注册失败！
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            //服务已取消注册。仅当您调用 NsdManager.unregisterService()并传入此侦听器时，才会被调用。
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // 注销失败。将调试代码放在此处以确定原因。
        }
    }
    
    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            //解析失败时调用。
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")

            if (serviceInfo.serviceName == mServiceName) {
                Log.d(TAG, "Same IP.")
                return
            }
            mService = serviceInfo
            val port: Int = serviceInfo.port
            val host: InetAddress = serviceInfo.host
        }
    }


    // 用于发现监听
    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started:$regType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            //找到服务
            Log.d(TAG, "Service discovery success $service")
            when {
                service.serviceType != SERVICE_TYPE -> // Service type is the string containing the protocol and
                    Log.d(TAG, "Unknown Service Type: ${service.serviceType}")
                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
                    // 连接对应的服务
                    Log.d(TAG, "Same machine: $mServiceName")
                service.serviceName.contains("Team Deck Server") -> nsdManger.resolveService(service, resolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // 当服务不在可用时回调
            Log.e(TAG, "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManger.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManger.stopServiceDiscovery(this)
        }
    }
```
**注意别使用SERVICE_TYPE="_http._tcp.local."
这个能正常的JmDNS的三方库中解析，但是在android自带的NSD是会解析失败的**

### 感谢您的阅读