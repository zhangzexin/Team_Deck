# 保护插件核心逻辑不被 R8/混淆压缩
-keep class com.zzx.plugin.** { *; }

# 保持 IPlugin 接口不被混淆
-keep interface com.zzx.common.plugin.IPlugin { *; }

# 如果使用了 Compose，通常需要保留这些
-keep class androidx.compose.** { *; }