package com.zzx.common.deck

/**
 * Deck 通用数据模型：按键布局与双端消息载荷。
 * 位于 commonMain，插件模块（compileOnly :shared）与宿主双端均可直接引用；
 * 全部为 Gson 友好的简单数据类，序列化后经 PLUGIN_CUSTOM(code=10) 信封的内层 data 字符串传输。
 */

/**
 * 一个可执行动作。
 * type 取值与参数约定（详见 desktopMain 的 ActionExecutor）：
 * - open_app     params: name (应用名，mac 上传给 `open -a`)
 * - open_url     params: url
 * - open_file    params: path
 * - shell        params: cmd (完整 shell 命令行), timeoutSec (可选，默认 30), background (可选 "true"=不等待)
 * - hotkey       params: keys (如 "cmd+f9"、"ctrl+r"，多组按顺序用逗号分隔 "cmd+1,cmd+2")
 * - applescript  params: script (osascript -e 的脚本内容，仅 macOS)
 * - menu_click   params: app (进程名), menu (菜单名), item (菜单项名，仅 macOS)
 * - activate_app params: name (要激活到前台的应用名)
 * - delay        params: ms (毫秒)
 * - multi        children: 按顺序执行的子动作列表
 */
data class DeckAction(
    val type: String,
    val params: Map<String, String> = emptyMap(),
    val children: List<DeckAction> = emptyList()
)

/** 一个 Deck 按键：显示信息 + 绑定动作。emoji 作为图标（跨端无资源依赖），color 为背景色 #RRGGBB。 */
data class DeckButton(
    val id: String,
    val label: String,
    val emoji: String = "",
    val color: String = "#455A64",
    val action: DeckAction? = null
)

/** 一页按键。 */
data class DeckPage(
    val id: String,
    val name: String,
    val buttons: List<DeckButton> = emptyList()
)

/** 完整布局：多页 + 网格列数。 */
data class DeckLayout(
    val pages: List<DeckPage> = emptyList(),
    val columns: Int = 4
)

/**
 * Deck 类插件在 PLUGIN_CUSTOM 内层交换的消息。
 * type: "layout"(桌面→手机 同步布局) / "press"(手机→桌面 按键按下) / "result"(桌面→手机 执行结果回执)
 * instanceId: 发送方实例代次，接收方用 PluginManager.checkMessageVersion 过滤跨 ClassLoader 僵尸消息
 */
data class DeckMessage(
    val type: String,
    val instanceId: Long = 0L,
    val layout: DeckLayout? = null,
    val pageId: String = "",
    val buttonId: String = "",
    val ok: Boolean = true,
    val detail: String = ""
)
