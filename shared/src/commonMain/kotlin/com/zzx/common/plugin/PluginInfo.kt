package com.zzx.common.plugin

/**
 * 插件元数据存储模型
 */
data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val apkUrl: String,
    val mainClass: String, // 实现 IPlugin 的类全名
    val icon: String? = null
)
