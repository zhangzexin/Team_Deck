package com.zzx.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.zzx.common.deck.DeckAction
import com.zzx.common.deck.DeckMessage
import com.zzx.common.plugin.IPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Android Studio 控制台插件：手机端一键触发 IDE 常用操作与 adb 工具。
 * - IDE 控制：激活 AS 窗口后注入快捷键（macOS/Windows 分平台 keymap），Sync 走 macOS 菜单点击；
 * - ADB 工具：不依赖 AS 前台，直接执行 adb 命令（截屏/录屏/清数据/重启应用等）；
 * - 桌面端 SettingsUI 可配置目标应用包名、截屏保存目录与 adb 路径。
 * 动作统一经 shared 的 ActionExecutor（反射入口）执行。
 */
class AndroidStudioPlugin : IPlugin {
    override val id: String = "android_studio_plugin"
    override val name: String = "AS 控制台"

    override var messageSender: ((String, String) -> Unit)? = null

    private val gson = Gson()
    private val instanceId = System.currentTimeMillis()
    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastResultState = MutableStateFlow("")

    private val osName = System.getProperty("os.name")?.lowercase() ?: ""
    private val isMacDesktop: Boolean get() = osName.contains("mac")

    private fun isDesktop(): Boolean {
        val vendor = System.getProperty("java.vendor") ?: ""
        return !vendor.contains("Android", ignoreCase = true) && !osName.contains("android", ignoreCase = true)
    }

    // ---------- 按键定义（双端共用同一份，无需同步） ----------

    private data class AsKey(
        val id: String,
        val label: String,
        val emoji: String,
        val color: String,
        val group: String
    )

    private val keys = listOf(
        // IDE 控制（需 macOS 辅助功能权限）
        AsKey("run", "Run", "▶️", "#2E7D32", "IDE 控制"),
        AsKey("debug", "Debug", "🐞", "#EF6C00", "IDE 控制"),
        AsKey("stop", "Stop", "⏹", "#C62828", "IDE 控制"),
        AsKey("build", "Build", "🔨", "#1565C0", "IDE 控制"),
        AsKey("sync", "Sync Gradle", "🔄", "#6A1B9A", "IDE 控制"),
        AsKey("find_action", "Find Action", "🔍", "#00838F", "IDE 控制"),
        // ADB 工具（无需权限，不依赖 AS 前台）
        AsKey("screenshot", "手机截屏", "📸", "#455A64", "ADB 工具"),
        AsKey("record", "录屏 30s", "🎥", "#455A64", "ADB 工具"),
        AsKey("clear_restart", "清数据重启", "🧹", "#C62828", "ADB 工具"),
        AsKey("force_restart", "重启应用", "🔁", "#EF6C00", "ADB 工具"),
        AsKey("logcat_clear", "清空 Logcat", "🗑", "#263238", "ADB 工具"),
        AsKey("layout_bounds", "布局边界", "📐", "#1565C0", "ADB 工具"),
        AsKey("show_touches", "显示触摸", "👆", "#00838F", "ADB 工具"),
        AsKey("key_back", "Back", "◀️", "#37474F", "ADB 工具"),
        AsKey("key_home", "Home", "🏠", "#37474F", "ADB 工具"),
        AsKey("key_wake", "亮屏", "💡", "#37474F", "ADB 工具")
    )

    // ---------- 配置（仅桌面端读写，java.util.prefs 持久化） ----------

    private val prefs by lazy { java.util.prefs.Preferences.userRoot().node("teamdeck/asplugin") }
    private var targetPackage: String
        get() = prefs.get("targetPackage", "")
        set(value) = prefs.put("targetPackage", value)
    private var screenshotDir: String
        get() = prefs.get("screenshotDir", System.getProperty("user.home") + "/Desktop")
        set(value) = prefs.put("screenshotDir", value)
    private var adbPath: String
        get() = prefs.get("adbPath", "adb")
        set(value) = prefs.put("adbPath", value)

    // ---------- 消息链路 ----------

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        if (actionId == "press" && !isDesktop()) {
            val msg = DeckMessage(type = "press", instanceId = instanceId, buttonId = params["buttonId"] ?: "")
            val envelope = mapOf(
                "code" to 10, // CodeEnum.PLUGIN_CUSTOM
                "msg" to "as_deck",
                "data" to mapOf("pluginId" to id, "data" to gson.toJson(msg))
            )
            messageSender?.invoke(id, gson.toJson(envelope))
        }
    }

    override fun onReceive(data: String) {
        val msg = try {
            gson.fromJson(data, DeckMessage::class.java)
        } catch (e: Exception) {
            null
        } ?: return

        when (msg.type) {
            "press" -> if (isDesktop()) handlePress(msg.buttonId)
            "result" -> if (!isDesktop()) {
                lastResultState.value = (if (msg.ok) "✓ " else "✗ ") + msg.detail
            }
        }
    }

    private fun handlePress(keyId: String) {
        val key = keys.find { it.id == keyId }
        if (key == null) {
            sendResult(false, "未知按键: $keyId")
            return
        }
        pluginScope.launch {
            val action = buildAction(keyId)
            if (action == null) {
                sendResult(false, "「${key.label}」在当前平台不可用或缺少配置")
                return@launch
            }
            val (ok, detail) = executeAction(action)
            sendResult(ok, "「${key.label}」${if (detail.isBlank()) if (ok) "已执行" else "执行失败" else detail}")
        }
    }

    private fun sendResult(ok: Boolean, detail: String) {
        lastResultState.value = (if (ok) "✓ " else "✗ ") + detail
        val msg = DeckMessage(type = "result", instanceId = instanceId, ok = ok, detail = detail)
        val envelope = mapOf(
            "code" to 10,
            "msg" to "as_deck",
            "data" to mapOf("pluginId" to id, "data" to gson.toJson(msg))
        )
        messageSender?.invoke(id, gson.toJson(envelope))
    }

    // ---------- 动作构造（桌面端） ----------

    /** IDE 快捷键前先激活 Android Studio 窗口，保证按键落到目标应用。 */
    private fun ideHotkey(macKeys: String, winKeys: String): DeckAction = DeckAction(
        type = "multi",
        children = listOf(
            DeckAction("activate_app", mapOf("name" to "Android Studio")),
            DeckAction("hotkey", mapOf("keys" to if (isMacDesktop) macKeys else winKeys))
        )
    )

    private fun adbShell(cmd: String, timeoutSec: Long = 30): DeckAction = DeckAction(
        type = "shell",
        params = mapOf("cmd" to cmd.replace("adb ", "\"${adbPath}\" "), "timeoutSec" to timeoutSec.toString())
    )

    private fun buildAction(keyId: String): DeckAction? {
        val pkg = targetPackage
        val dir = screenshotDir
        return when (keyId) {
            // IDE 控制（macOS 默认 keymap / Windows 默认 keymap）
            "run" -> ideHotkey("ctrl+r", "shift+f10")
            "debug" -> ideHotkey("ctrl+d", "shift+f9")
            "stop" -> ideHotkey("cmd+f2", "ctrl+f2")
            "build" -> ideHotkey("cmd+f9", "ctrl+f9")
            "find_action" -> ideHotkey("cmd+shift+a", "ctrl+shift+a")
            "sync" -> if (isMacDesktop) DeckAction(
                "menu_click",
                mapOf("app" to "Android Studio", "menu" to "File", "item" to "Sync Project with Gradle Files")
            ) else null // Windows 默认 keymap 无 Sync 快捷键，菜单点击仅支持 macOS

            // ADB 工具
            "screenshot" -> adbShell("adb exec-out screencap -p > \"$dir/as_screen_\$(date +%H%M%S).png\"")
            "record" -> adbShell(
                "adb shell screenrecord --time-limit 30 /sdcard/teamdeck_record.mp4 && " +
                        "adb pull /sdcard/teamdeck_record.mp4 \"$dir/as_record_\$(date +%H%M%S).mp4\" && " +
                        "adb shell rm /sdcard/teamdeck_record.mp4",
                timeoutSec = 60
            )
            "clear_restart" -> if (pkg.isBlank()) null else adbShell(
                "adb shell pm clear $pkg && adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1"
            )
            "force_restart" -> if (pkg.isBlank()) null else adbShell(
                "adb shell am force-stop $pkg && adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1"
            )
            "logcat_clear" -> adbShell("adb logcat -c")
            "layout_bounds" -> adbShell(
                "if [ \"\$(adb shell getprop debug.layout)\" = \"true\" ]; then " +
                        "adb shell setprop debug.layout false; else adb shell setprop debug.layout true; fi; " +
                        "adb shell service call activity 1599295570 > /dev/null"
            )
            "show_touches" -> adbShell(
                "cur=\$(adb shell settings get system show_touches); " +
                        "if [ \"\$cur\" = \"1\" ]; then adb shell settings put system show_touches 0; " +
                        "else adb shell settings put system show_touches 1; fi"
            )
            "key_back" -> adbShell("adb shell input keyevent 4")
            "key_home" -> adbShell("adb shell input keyevent 3")
            "key_wake" -> adbShell("adb shell input keyevent 224")
            else -> null
        }
    }

    /** 反射调用 shared desktopMain 的 ActionExecutor（Android 编译期不可见，运行时由宿主提供）。 */
    private fun executeAction(action: DeckAction): Pair<Boolean, String> = try {
        val clazz = Class.forName("com.zzx.common.deck.ActionExecutor")
        val method = clazz.getMethod("execute", String::class.java)
        val resultJson = method.invoke(null, gson.toJson(action)) as String
        val result = gson.fromJson(resultJson, ExecResultPayload::class.java)
        Pair(result?.ok ?: false, result?.detail ?: "")
    } catch (e: Exception) {
        Pair(false, "executor error: ${e.message}")
    }

    private data class ExecResultPayload(val ok: Boolean = false, val detail: String? = "")

    override fun onDestroy() {
        pluginScope.cancel()
    }

    // ---------- 手机端 UI ----------

    @Composable
    override fun AppUI() {
        var showBoard by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF3DDC84), Color(0xFF073042))
                    )
                )
                .clickable { showBoard = true },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🤖", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("AS 控制台", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showBoard) {
            Dialog(
                onDismissRequest = { showBoard = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AsBoard(onClose = { showBoard = false })
            }
        }
    }

    @Composable
    private fun AsBoard(onClose: () -> Unit) {
        val lastResult by lastResultState.collectAsState()
        val grouped = keys.groupBy { it.group }

        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF10151A)) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Android Studio 控制台",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (group, groupKeys) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                group,
                                color = Color(0xFF4FC3F7),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(groupKeys, key = { it.id }) { key ->
                            AsKeyCard(key) {
                                onTrigger("press", mapOf("buttonId" to key.id))
                            }
                        }
                    }
                }

                if (lastResult.isNotBlank()) {
                    Surface(
                        color = Color(0xFF263238),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            lastResult,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AsKeyCard(key: AsKey, onPress: () -> Unit) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = parseHexColor(key.color, Color(0xFF455A64)),
            modifier = Modifier
                .aspectRatio(1f)
                .clickable { onPress() }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(key.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    key.label,
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp
                )
            }
        }
    }

    // ---------- 桌面端 UI ----------

    @Composable
    override fun DesktopUI() {
        val lastResult by lastResultState.collectAsState()
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = Color(0xFF3DDC84)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🤖", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("AS 控制台", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "${keys.size} 个操作",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            if (lastResult.isNotBlank()) {
                Text(
                    lastResult,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    override fun SettingsUI() {
        var pkg by remember { mutableStateOf(targetPackage) }
        var dir by remember { mutableStateOf(screenshotDir) }
        var adb by remember { mutableStateOf(adbPath) }
        var savedTip by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text("Android Studio 控制台设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "IDE 控制类按键通过模拟快捷键实现，macOS 需在「系统设置 → 隐私与安全性 → 辅助功能」中授权本应用（或运行它的 java 进程）。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pkg,
                onValueChange = { pkg = it },
                label = { Text("目标应用包名（清数据/重启用，如 com.example.app）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dir,
                onValueChange = { dir = it },
                label = { Text("截屏/录屏保存目录") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = adb,
                onValueChange = { adb = it },
                label = { Text("adb 路径（默认使用 PATH 中的 adb）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    targetPackage = pkg.trim()
                    screenshotDir = dir.trim()
                    adbPath = adb.trim().ifBlank { "adb" }
                    savedTip = "已保存"
                }) { Text("保存") }
                Spacer(modifier = Modifier.width(12.dp))
                if (savedTip.isNotBlank()) {
                    Text(savedTip, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }
    }

    private fun parseHexColor(hex: String, fallback: Color): Color = try {
        Color(0xFF000000L or hex.removePrefix("#").toLong(16))
    } catch (e: Exception) {
        fallback
    }
}
