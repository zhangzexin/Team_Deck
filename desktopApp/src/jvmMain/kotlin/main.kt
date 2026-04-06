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
                    // 使用万能探测加载器，先从原始路径读取 ID 和信息
                    val loader = PluginLoader()
                    val p = loader.loadPluginAuto(it)

                    p?.let { plugin ->
                        // 1. 如果是更新/安装，先停止本地旧实例，不需要通知手机端（手机端接收新文件时会自动处理替换）
                        // 注意：此处 trueUninstall 必须为 false，否则会连同 LocalAppData 里的主文件一起删掉
                        com.zzx.common.plugin.PluginManager.removePlugin(plugin.id, notifyRemote = false, trueUninstall = false)
                        
                        // 2. 将文件持久化到本地目录
                        FileUtils.copyFileTo(it, ApplicationInfo.LocalAppData.toString())
                        val localCopy = File(ApplicationInfo.LocalAppData.toString(), File(it).name).absolutePath
                        
                        // 3. 加载本地副本并启动
                        val finalPlugin = loader.loadPluginAuto(localCopy)
                        finalPlugin?.let { 
                            com.zzx.common.plugin.PluginManager.addPlugin(it) 
                            // 4. 重大修复：在本地加载成功后，立即触发向手机端同步该新文件 (即拖即传)
                            NsdManagerUtils.Instance.sendFile(localCopy)
                        }
                    }
                    println("Desktop plugin handled for: $it")
                }
            }
        }
    }
}