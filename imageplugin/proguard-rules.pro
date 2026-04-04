# 保护插件核心逻辑不被压缩
-keep class com.zzx.plugin.** { *; }
-keep interface com.zzx.common.plugin.IPlugin { *; }
