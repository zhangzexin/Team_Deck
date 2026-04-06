# Network Troubleshooting in Certain WiFi Environments | 在部分 WiFi 环境下出现的问题

English | [中文](#中文)

This document addresses issues where file uploads via WebSockets might disconnect in certain router environments.

---

<a name="中文"></a>

# 在部分 WiFi 环境下出现的问题

在部分环境下会出现通过websocket进行文件上传，接收方会出现接收断开，是由于路由器造成，目前方案是降低上传间隔，或者更换为UDP，再或者重写上传规则用0x1的信道试试是否有限制
