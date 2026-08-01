package com.zzx.common.deck

import com.google.gson.Gson
import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 桌面端通用动作执行器（仅 desktopMain，运行于宿主 JVM）。
 *
 * 插件模块编译走 Android target 看不到本类，需通过一次反射调用稳定入口：
 *   Class.forName("com.zzx.common.deck.ActionExecutor")
 *        .getMethod("execute", String::class.java)
 *        .invoke(null, actionJson)
 * 入参为 DeckAction 的 JSON，返回 {"ok":Boolean,"detail":String} 的 JSON。
 *
 * macOS 注意：hotkey 依赖 java.awt.Robot，首次使用需在
 * 系统设置 → 隐私与安全性 → 辅助功能 中授权运行本应用的 java 进程。
 */
object ActionExecutor {
    private val gson = Gson()
    private val osName = System.getProperty("os.name").lowercase()
    private val isMac = osName.contains("mac")
    private val isWindows = osName.contains("win")

    data class ExecResult(val ok: Boolean, val detail: String = "")

    /** 稳定反射入口：actionJson 为 DeckAction 序列化结果。 */
    @JvmStatic
    fun execute(actionJson: String): String {
        val result = try {
            val action = gson.fromJson(actionJson, DeckAction::class.java)
                ?: return gson.toJson(ExecResult(false, "invalid action json"))
            runAction(action)
        } catch (e: Exception) {
            ExecResult(false, "execute failed: ${e.message}")
        }
        return gson.toJson(result)
    }

    fun runAction(action: DeckAction): ExecResult {
        return try {
            when (action.type) {
            "open_app" -> openApp(action.params.req("name"))
            "open_url" -> openTarget(action.params.req("url"))
            "open_file" -> openTarget(action.params.req("path"))
            "shell" -> shell(
                action.params.req("cmd"),
                action.params["timeoutSec"]?.toLongOrNull() ?: 30L,
                action.params["background"] == "true"
            )
            "hotkey" -> hotkey(action.params.req("keys"))
            "applescript" -> applescript(action.params.req("script"))
            "menu_click" -> menuClick(
                action.params.req("app"),
                action.params.req("menu"),
                action.params.req("item")
            )
            "activate_app" -> activateApp(action.params.req("name"))
            "delay" -> {
                Thread.sleep(action.params["ms"]?.toLongOrNull() ?: 300L)
                ExecResult(true)
            }
            "multi" -> {
                for (child in action.children) {
                    val r = runAction(child)
                    if (!r.ok) return r
                }
                ExecResult(true, "multi: ${action.children.size} actions done")
            }
            else -> ExecResult(false, "unknown action type: ${action.type}")
        }
        } catch (e: MissingParamException) {
            ExecResult(false, e.message ?: "missing param")
        } catch (e: Exception) {
            ExecResult(false, "${action.type} failed: ${e.message}")
        }
    }

    // ---------- 具体动作 ----------

    private fun openApp(name: String): ExecResult = if (isMac) {
        runProcess(listOf("open", "-a", name))
    } else if (isWindows) {
        runProcess(listOf("cmd", "/c", "start", "", name))
    } else {
        runProcess(listOf("xdg-open", name))
    }

    private fun openTarget(target: String): ExecResult = if (isMac) {
        runProcess(listOf("open", target))
    } else if (isWindows) {
        runProcess(listOf("cmd", "/c", "start", "", target))
    } else {
        runProcess(listOf("xdg-open", target))
    }

    private fun shell(cmd: String, timeoutSec: Long, background: Boolean): ExecResult {
        val argv = if (isWindows) listOf("cmd", "/c", cmd) else listOf("/bin/zsh", "-lc", cmd)
        val process = ProcessBuilder(argv).redirectErrorStream(true).start()
        if (background) {
            return ExecResult(true, "started in background")
        }
        val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return ExecResult(false, "timeout after ${timeoutSec}s")
        }
        val output = process.inputStream.bufferedReader().readText().trim().take(500)
        return if (process.exitValue() == 0) {
            ExecResult(true, output)
        } else {
            ExecResult(false, "exit ${process.exitValue()}: $output")
        }
    }

    private fun applescript(script: String): ExecResult {
        if (!isMac) return ExecResult(false, "applescript is macOS only")
        return runProcess(listOf("osascript", "-e", script))
    }

    private fun activateApp(name: String): ExecResult {
        if (!isMac) {
            // Windows 上没有等价的通用激活方式，退化为尝试启动（已在前台则无副作用）
            return openApp(name)
        }
        val r = runProcess(listOf("osascript", "-e", "tell application \"$name\" to activate"))
        // 给窗口切换留出时间，紧随其后的 hotkey 才能落到目标应用
        Thread.sleep(400)
        return r
    }

    private fun menuClick(app: String, menu: String, item: String): ExecResult {
        if (!isMac) return ExecResult(false, "menu_click is macOS only")
        activateApp(app)
        val script = "tell application \"System Events\" to tell process \"$app\" " +
                "to click menu item \"$item\" of menu \"$menu\" of menu bar 1"
        return runProcess(listOf("osascript", "-e", script))
    }

    /**
     * 解析 "cmd+shift+a" 形式的组合键并用 Robot 注入；多组按顺序用逗号分隔。
     * 修饰键先按后释放（逆序），保证组合语义。
     */
    private fun hotkey(keys: String): ExecResult {
        val robot = try {
            Robot()
        } catch (e: Exception) {
            return ExecResult(false, "Robot unavailable (headless or no permission): ${e.message}")
        }
        for (combo in keys.split(",").map { it.trim() }.filter { it.isNotEmpty() }) {
            val codes = combo.split("+").map { token ->
                keyCode(token.trim().lowercase())
                    ?: return ExecResult(false, "unknown key: $token in \"$combo\"")
            }
            codes.forEach { robot.keyPress(it) }
            codes.reversed().forEach { robot.keyRelease(it) }
            robot.delay(80)
        }
        return ExecResult(true, "sent: $keys")
    }

    private fun keyCode(token: String): Int? = when (token) {
        "cmd", "meta", "win" -> KeyEvent.VK_META
        "ctrl", "control" -> KeyEvent.VK_CONTROL
        "alt", "opt", "option" -> KeyEvent.VK_ALT
        "shift" -> KeyEvent.VK_SHIFT
        "enter", "return" -> KeyEvent.VK_ENTER
        "esc", "escape" -> KeyEvent.VK_ESCAPE
        "tab" -> KeyEvent.VK_TAB
        "space" -> KeyEvent.VK_SPACE
        "backspace" -> KeyEvent.VK_BACK_SPACE
        "delete", "del" -> KeyEvent.VK_DELETE
        "up" -> KeyEvent.VK_UP
        "down" -> KeyEvent.VK_DOWN
        "left" -> KeyEvent.VK_LEFT
        "right" -> KeyEvent.VK_RIGHT
        "home" -> KeyEvent.VK_HOME
        "end" -> KeyEvent.VK_END
        "pageup" -> KeyEvent.VK_PAGE_UP
        "pagedown" -> KeyEvent.VK_PAGE_DOWN
        else -> when {
            token.length == 1 && token[0] in 'a'..'z' -> KeyEvent.VK_A + (token[0] - 'a')
            token.length == 1 && token[0] in '0'..'9' -> KeyEvent.VK_0 + (token[0] - '0')
            token.matches(Regex("f([1-9]|1[0-2])")) -> KeyEvent.VK_F1 + (token.substring(1).toInt() - 1)
            else -> null
        }
    }

    // ---------- 工具 ----------

    private fun runProcess(argv: List<String>): ExecResult {
        val process = ProcessBuilder(argv).redirectErrorStream(true).start()
        val finished = process.waitFor(15, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return ExecResult(false, "timeout: ${argv.joinToString(" ")}")
        }
        val output = process.inputStream.bufferedReader().readText().trim().take(500)
        return if (process.exitValue() == 0) {
            ExecResult(true, output)
        } else {
            ExecResult(false, "exit ${process.exitValue()}: $output")
        }
    }

    private class MissingParamException(name: String) : Exception("missing param: $name")

    private fun Map<String, String>.req(name: String): String =
        this[name]?.takeIf { it.isNotBlank() } ?: throw MissingParamException(name)
}
