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
        println(Paths.get(System.getProperty("user.home"), "AppData", "Local").toString())
        val rememberCoroutineScope = rememberCoroutineScope()
        DropBoxPanel(modifier = Modifier.size(300.dp), window = window) {
            rememberCoroutineScope.launch(Dispatchers.IO) {
                if (it.endsWith(".apk") || it.endsWith(".aar") || it.endsWith(".jar")) {
                    val localCopy = File(ApplicationInfo.LocalAppData.toString(), File(it).name).absolutePath
                    FileUtils.copyFileTo(it, ApplicationInfo.LocalAppData.toString())
                    
                    // 异步发送原始文件
                    NsdManagerUtils.Instance.sendFile(it)

                    // 加载本地副本以在 Desktop 端展示 UI (避免 Windows 文件锁冲突)
                    val loader = PluginLoader()
                    val candidates = listOf("com.zzx.plugin.ImagePlugin", "com.zzx.plugin.SamplePlugin")
                    var loadedPlugin: com.zzx.common.plugin.IPlugin? = null
                    
                    for (className in candidates) {
                        try {
                            val p = loader.loadPlugin(localCopy, className)
                            if (p != null) {
                                loadedPlugin = p
                                println("Successfully loaded plugin class from main.kt: $className")
                                break
                            }
                        } catch (e: Exception) {
                            // 继续尝试
                        }
                    }

                    loadedPlugin?.let { p ->
                        com.zzx.common.plugin.PluginManager.addPlugin(p)
                    }
                    println("Desktop plugin handled from: $localCopy")
                }
            }
        }
    }
}