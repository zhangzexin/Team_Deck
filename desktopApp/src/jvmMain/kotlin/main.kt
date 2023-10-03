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
import com.zzx.desktop.teamdeck.ui.App1
import com.zzx.desktop.teamdeck.ui.DropBoxPanel
import com.zzx.desktop.teamdeck.utils.FileUtils
import com.zzx.desktop.teamdeck.utils.NsdManagerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
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
                if (it.endsWith(".apk")) {
                    FileUtils.copyFileTo(it, ApplicationInfo.LocalAppData.toString())
                    NsdManagerUtils.Instance.sendFile(it)
                    println(it)
                }
            }
        }
    }
}