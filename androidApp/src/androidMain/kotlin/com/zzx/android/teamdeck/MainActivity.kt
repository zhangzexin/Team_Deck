package com.zzx.android.teamdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.unit.dp
import com.zzx.android.teamdeck.ui.components.TeamDeckApp
import com.zzx.android.teamdeck.ui.theme.AppTheme
import com.zzx.android.teamdeck.viewmodel.MainViewModel
import com.zzx.common.plugin.PluginLoader
import com.zzx.common.socket.InputFileHandler
import com.zzx.common.socket.InputFileHandler.Companion.plugindir


val baseMaxWidth = 360.dp
val baseMaxHeight = 764.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plugindir = this.getFileStreamPath("plugin").path
        
        // 确保插件目录存在
        val dir = java.io.File(plugindir!!)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val loader = PluginLoader(this)
        InputFileHandler.pluginLoader = loader
        
        // 插件持久化：启动时从本地目录加载文件
        com.zzx.common.plugin.PluginManager.loadPluginsFromDir(plugindir!!, loader)

        setContent {
            AppTheme {
                val viewModel: MainViewModel by viewModels()
                TeamDeckApp(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}

