import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zzx.desktop.teamdeck.ApplicationInfo
import com.zzx.desktop.teamdeck.plugin.PluginManager
import com.zzx.common.plugin.PluginLoader
import com.zzx.desktop.teamdeck.ui.App1
import com.zzx.desktop.teamdeck.ui.DropBoxPanel
import com.zzx.desktop.teamdeck.utils.FileUtils
import com.zzx.desktop.teamdeck.utils.NsdManagerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.io.File
import java.nio.file.Paths

//fun main() = application {
//    Window(onCloseRequest = ::exitApplication) {
//        MainView()
//    }
//}

fun main() = application {
    val rememberWindowState = rememberWindowState(size = DpSize(1800.dp, 1200.dp))
    Window(onCloseRequest = ::exitApplication, state = rememberWindowState) {
        window.addContainerListener(object : ContainerListener {
            override fun componentAdded(p0: ContainerEvent?) {
                println(p0.toString())
            }

            override fun componentRemoved(p0: ContainerEvent?) {
                println(p0.toString())
            }
        })
//        App()
        MaterialTheme {
            App1()
        }
        
        // 插件持久化：启动时从本地目录加载已安装的插件
        val pluginDir = ApplicationInfo.LocalAppData.toFile()
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }
        com.zzx.common.plugin.PluginManager.loadPluginsFromDir(
            pluginDir.absolutePath, 
            PluginLoader()
        )

        println(Paths.get(System.getProperty("user.home"), "AppData", "Local").toString())
        val rememberCoroutineScope = rememberCoroutineScope()
        DropBoxPanel(modifier = Modifier.size(300.dp), window = window) {
            rememberCoroutineScope.launch(Dispatchers.IO) {
                if (it.endsWith(".apk") || it.endsWith(".aar") || it.endsWith(".jar")) {
                    val localCopy = File(ApplicationInfo.LocalAppData.toString(), File(it).name).absolutePath
                    FileUtils.copyFileTo(it, ApplicationInfo.LocalAppData.toString())

                    // 加载本地副本以在 Desktop 端展示 UI (避免 Windows 文件锁冲突)
                    val loader = PluginLoader()
                    // 使用万能探测加载器，自动从包内读取 plugin.properties
                    val loadedPlugin = loader.loadPluginAuto(localCopy)

                    loadedPlugin?.let { p ->
                        // 1. 如果是更新插件，先通知手机端卸载旧版本
                        com.zzx.common.plugin.PluginManager.removePlugin(p.id, notifyRemote = true)
                        
                        // 2. 异步同步原始文件到手机
                        NsdManagerUtils.Instance.sendFile(it)

                        // 3. 在 Desktop 端添加/启动新插件
                        com.zzx.common.plugin.PluginManager.addPlugin(p)
                    }
                    println("Desktop plugin handled from: $localCopy")
                }
            }
        }
    }
}