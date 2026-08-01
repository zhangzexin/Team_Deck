package com.zzx.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.zzx.common.deck.DeckButton
import com.zzx.common.deck.DeckLayout
import com.zzx.common.deck.DeckMessage
import com.zzx.common.deck.DeckPage
import com.zzx.common.plugin.IPlugin
import com.zzx.common.plugin.PluginManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Deck 控制台插件：类 Stream Deck 的可配置按键网格。
 * 手机端：点击卡片弹出全屏分页按键网格，按下即触发；
 * 桌面端：SettingsUI 可视化编辑按键与动作，收到 press 后经 shared 的
 * ActionExecutor（反射入口）在本机执行 open_app/shell/hotkey/applescript 等动作。
 */
class DeckPlugin : IPlugin {
    override val id: String = "deck_plugin"
    override val name: String = "Deck 控制台"

    override var messageSender: ((String, String) -> Unit)? = null

    private val gson = Gson()
    private val instanceId = System.currentTimeMillis()
    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 布局：桌面端为权威（持久化到磁盘），手机端为同步副本
    private val layoutState = MutableStateFlow(defaultLayout())
    // 最近一次执行结果（双端展示用）
    private val lastResultState = MutableStateFlow("")

    private val layoutFile: File
        get() = File(
            System.getProperty("user.home"),
            "AppData/Roaming/TeamDeck/deck_layout.json"
        )

    init {
        if (isDesktop()) {
            loadLayoutFromDisk()
            startSyncLoop()
        }
    }

    private fun isDesktop(): Boolean {
        val vendor = System.getProperty("java.vendor") ?: ""
        val osName = System.getProperty("os.name") ?: ""
        return !vendor.contains("Android", ignoreCase = true) && !osName.contains("android", ignoreCase = true)
    }

    // ---------- 持久化与同步 ----------

    private fun loadLayoutFromDisk() {
        try {
            if (layoutFile.exists()) {
                val loaded = gson.fromJson(layoutFile.readText(), DeckLayout::class.java)
                if (loaded != null && loaded.pages.isNotEmpty()) {
                    layoutState.value = loaded
                }
            }
        } catch (e: Exception) {
            println("DeckPlugin: failed to load layout: ${e.message}")
        }
    }

    private fun saveLayoutToDisk() {
        try {
            layoutFile.parentFile?.mkdirs()
            layoutFile.writeText(gson.toJson(layoutState.value))
        } catch (e: Exception) {
            println("DeckPlugin: failed to save layout: ${e.message}")
        }
    }

    /** 桌面端周期性推送布局（连接建立后手机端即持有最新配置，无需手机端持久化）。 */
    private fun startSyncLoop() {
        val job = pluginScope.launch {
            while (isActive) {
                pushLayout()
                delay(3000)
            }
        }
        PluginManager.registerJob(id, job)
    }

    private fun sendDeckMessage(msg: DeckMessage) {
        val envelope = mapOf(
            "code" to 10, // CodeEnum.PLUGIN_CUSTOM
            "msg" to "deck",
            "data" to mapOf(
                "pluginId" to id,
                "data" to gson.toJson(msg)
            )
        )
        messageSender?.invoke(id, gson.toJson(envelope))
    }

    private fun pushLayout() {
        sendDeckMessage(DeckMessage(type = "layout", instanceId = instanceId, layout = layoutState.value))
    }

    private fun sendResult(ok: Boolean, detail: String) {
        lastResultState.value = (if (ok) "✓ " else "✗ ") + detail
        sendDeckMessage(DeckMessage(type = "result", instanceId = instanceId, ok = ok, detail = detail))
    }

    /** 桌面端编辑入口：更新布局 + 落盘 + 立即推送。 */
    private fun updateLayout(transform: (DeckLayout) -> DeckLayout) {
        layoutState.value = transform(layoutState.value)
        saveLayoutToDisk()
        pushLayout()
    }

    // ---------- 消息处理 ----------

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        // 手机端按键按下：actionId 形如 "press"，params 携带 pageId/buttonId
        if (actionId == "press" && !isDesktop()) {
            sendDeckMessage(
                DeckMessage(
                    type = "press",
                    instanceId = instanceId,
                    pageId = params["pageId"] ?: "",
                    buttonId = params["buttonId"] ?: ""
                )
            )
        }
    }

    override fun onReceive(data: String) {
        val msg = try {
            gson.fromJson(data, DeckMessage::class.java)
        } catch (e: Exception) {
            null
        } ?: return

        when (msg.type) {
            "layout" -> if (!isDesktop()) {
                // 过滤跨 ClassLoader 的僵尸实例消息
                if (PluginManager.shouldProcessMessage(id, msg.instanceId)) {
                    msg.layout?.let { layoutState.value = it }
                }
            }
            "press" -> if (isDesktop()) {
                handlePress(msg.pageId, msg.buttonId)
            }
            "result" -> if (!isDesktop()) {
                lastResultState.value = (if (msg.ok) "✓ " else "✗ ") + msg.detail
            }
        }
    }

    private fun handlePress(pageId: String, buttonId: String) {
        val button = layoutState.value.pages.find { it.id == pageId }
            ?.buttons?.find { it.id == buttonId }
        if (button == null) {
            sendResult(false, "按键不存在: $buttonId")
            return
        }
        val action = button.action
        if (action == null) {
            sendResult(false, "「${button.label}」未绑定动作")
            return
        }
        pluginScope.launch {
            val (ok, detail) = executeAction(action)
            sendResult(ok, "「${button.label}」${if (detail.isBlank()) if (ok) "已执行" else "执行失败" else detail}")
        }
    }

    /**
     * 反射调用 shared desktopMain 的 ActionExecutor（插件编译走 Android target 看不到该类，
     * 运行时由宿主 ClassLoader 提供；JSON 字符串作为跨 ClassLoader 边界）。
     */
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
        PluginManager.cancelJob(id)
        pluginScope.cancel()
    }

    // ---------- 手机端 UI ----------

    @Composable
    override fun AppUI() {
        val layout by layoutState.collectAsState()
        var showDeck by remember { mutableStateOf(false) }

        // 宿主网格中的入口卡片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF37474F), Color(0xFF102027))
                    )
                )
                .clickable { showDeck = true },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎛", fontSize = 34.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Deck", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${layout.pages.sumOf { it.buttons.size }} 键",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }

        if (showDeck) {
            Dialog(
                onDismissRequest = { showDeck = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                DeckBoard(onClose = { showDeck = false })
            }
        }
    }

    /** 全屏按键面板：页签 + 按键网格 + 结果条。 */
    @Composable
    private fun DeckBoard(onClose: () -> Unit) {
        val layout by layoutState.collectAsState()
        val lastResult by lastResultState.collectAsState()
        var pageIndex by remember { mutableStateOf(0) }
        val safeIndex = pageIndex.coerceIn(0, (layout.pages.size - 1).coerceAtLeast(0))
        val page = layout.pages.getOrNull(safeIndex)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF15191E)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // 顶栏：页签 + 关闭
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f)) {
                        layout.pages.forEachIndexed { index, p ->
                            val selected = index == safeIndex
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) Color(0xFF4FC3F7) else Color(0xFF263238),
                                modifier = Modifier.padding(end = 8.dp).clickable { pageIndex = index }
                            ) {
                                Text(
                                    p.name,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = if (selected) Color(0xFF102027) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 按键网格
                if (page == null || page.buttons.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("暂无按键，请在电脑端配置", color = Color.White.copy(alpha = 0.5f))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(layout.columns.coerceIn(2, 8)),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(page.buttons, key = { it.id }) { button ->
                            DeckKey(button) {
                                onTrigger("press", mapOf("pageId" to page.id, "buttonId" to button.id))
                            }
                        }
                    }
                }

                // 结果条
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
    private fun DeckKey(button: DeckButton, onPress: () -> Unit) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = parseHexColor(button.color, Color(0xFF455A64)),
            modifier = Modifier
                .aspectRatio(1f)
                .clickable { onPress() }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(button.emoji.ifBlank { "▫️" }, fontSize = 26.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    button.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
        }
    }

    // ---------- 桌面端 UI ----------

    @Composable
    override fun DesktopUI() {
        val layout by layoutState.collectAsState()
        val lastResult by lastResultState.collectAsState()
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎛", fontSize = 22.sp)
            Text("Deck 控制台", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "${layout.pages.size} 页 / ${layout.pages.sumOf { it.buttons.size }} 键",
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun SettingsUI() {
        val layout by layoutState.collectAsState()
        var pageIndex by remember { mutableStateOf(0) }
        val safeIndex = pageIndex.coerceIn(0, (layout.pages.size - 1).coerceAtLeast(0))
        val page = layout.pages.getOrNull(safeIndex)
        var editingButton by remember { mutableStateOf<DeckButton?>(null) }
        var isNewButton by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text("Deck 按键配置", style = MaterialTheme.typography.titleMedium)
            Text(
                "配置保存后会自动同步到手机端（配置文件：~/AppData/Roaming/TeamDeck/deck_layout.json）",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 页管理
            Row(verticalAlignment = Alignment.CenterVertically) {
                layout.pages.forEachIndexed { index, p ->
                    FilterChip(
                        selected = index == safeIndex,
                        onClick = { pageIndex = index },
                        label = { Text(p.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                IconButton(onClick = {
                    updateLayout { l ->
                        l.copy(pages = l.pages + DeckPage("page_${System.currentTimeMillis()}", "新页面"))
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "添加页")
                }
                if (layout.pages.size > 1 && page != null) {
                    IconButton(onClick = {
                        updateLayout { l -> l.copy(pages = l.pages.filterNot { it.id == page.id }) }
                        pageIndex = 0
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除当前页")
                    }
                }
            }

            if (page != null) {
                var pageName by remember(page.id) { mutableStateOf(page.name) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    OutlinedTextField(
                        value = pageName,
                        onValueChange = { pageName = it },
                        label = { Text("页名称") },
                        singleLine = true,
                        modifier = Modifier.width(200.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        updateLayout { l ->
                            l.copy(pages = l.pages.map { if (it.id == page.id) it.copy(name = pageName) else it })
                        }
                    }) { Text("重命名") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // 按键列表
                page.buttons.forEachIndexed { index, button ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(button.color, Color.Gray)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(button.emoji, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(button.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    actionSummary(button.action),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                if (index > 0) updateLayout { l -> l.movePageButton(page.id, index, index - 1) }
                            }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移") }
                            IconButton(onClick = {
                                if (index < page.buttons.size - 1) updateLayout { l -> l.movePageButton(page.id, index, index + 1) }
                            }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移") }
                            IconButton(onClick = {
                                isNewButton = false
                                editingButton = button
                            }) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
                            IconButton(onClick = {
                                updateLayout { l ->
                                    l.copy(pages = l.pages.map {
                                        if (it.id == page.id) it.copy(buttons = it.buttons.filterNot { b -> b.id == button.id }) else it
                                    })
                                }
                            }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    isNewButton = true
                    editingButton = DeckButton(
                        id = "btn_${System.currentTimeMillis()}",
                        label = "新按键",
                        emoji = "⭐",
                        action = DeckAction("open_app", mapOf("name" to ""))
                    )
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加按键")
                }
            }
        }

        // 编辑对话框
        editingButton?.let { editing ->
            ButtonEditDialog(
                initial = editing,
                onDismiss = { editingButton = null },
                onSave = { saved ->
                    val targetPageId = page?.id ?: return@ButtonEditDialog
                    updateLayout { l ->
                        l.copy(pages = l.pages.map { p ->
                            if (p.id != targetPageId) p
                            else if (isNewButton) p.copy(buttons = p.buttons + saved)
                            else p.copy(buttons = p.buttons.map { if (it.id == saved.id) saved else it })
                        })
                    }
                    editingButton = null
                }
            )
        }
    }

    /** 每种动作类型在编辑器中需要的参数字段（key → 显示名）。 */
    private val actionTypeFields: Map<String, List<Pair<String, String>>> = mapOf(
        "open_app" to listOf("name" to "应用名 (如 Safari)"),
        "open_url" to listOf("url" to "网址"),
        "open_file" to listOf("path" to "文件路径"),
        "shell" to listOf("cmd" to "Shell 命令", "timeoutSec" to "超时秒数(默认30)"),
        "hotkey" to listOf("keys" to "快捷键 (如 cmd+shift+a)"),
        "applescript" to listOf("script" to "AppleScript 脚本"),
        "menu_click" to listOf("app" to "应用进程名", "menu" to "菜单名", "item" to "菜单项"),
        "activate_app" to listOf("name" to "应用名"),
        "delay" to listOf("ms" to "毫秒")
    )

    private val presetColors = listOf(
        "#1565C0", "#2E7D32", "#6A1B9A", "#C62828",
        "#EF6C00", "#00838F", "#455A64", "#263238"
    )

    @Composable
    private fun ButtonEditDialog(
        initial: DeckButton,
        onDismiss: () -> Unit,
        onSave: (DeckButton) -> Unit
    ) {
        var label by remember { mutableStateOf(initial.label) }
        var emoji by remember { mutableStateOf(initial.emoji) }
        var color by remember { mutableStateOf(initial.color) }
        var actionType by remember { mutableStateOf(initial.action?.type ?: "open_app") }
        var params by remember { mutableStateOf(initial.action?.params ?: emptyMap()) }
        var typeMenuOpen by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("编辑按键") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row {
                        OutlinedTextField(
                            value = label, onValueChange = { label = it },
                            label = { Text("名称") }, singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = emoji, onValueChange = { emoji = it },
                            label = { Text("图标") }, singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 颜色选择
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("颜色: ", fontSize = 13.sp)
                        presetColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex, Color.Gray))
                                    .border(
                                        width = if (color == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { color = hex }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 动作类型下拉
                    Box {
                        OutlinedButton(onClick = { typeMenuOpen = true }) {
                            Text("动作类型: ${actionTypeName(actionType)}")
                        }
                        DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                            actionTypeFields.keys.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(actionTypeName(type)) },
                                    onClick = {
                                        actionType = type
                                        params = emptyMap()
                                        typeMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 动态参数字段
                    (actionTypeFields[actionType] ?: emptyList()).forEach { (key, hint) ->
                        OutlinedTextField(
                            value = params[key] ?: "",
                            onValueChange = { params = params + (key to it) },
                            label = { Text(hint) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(
                        initial.copy(
                            label = label,
                            emoji = emoji,
                            color = color,
                            action = DeckAction(actionType, params)
                        )
                    )
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    }

    // ---------- 工具 ----------

    private fun actionTypeName(type: String): String = when (type) {
        "open_app" -> "打开应用"
        "open_url" -> "打开网址"
        "open_file" -> "打开文件"
        "shell" -> "Shell 命令"
        "hotkey" -> "快捷键"
        "applescript" -> "AppleScript"
        "menu_click" -> "点击菜单"
        "activate_app" -> "激活应用"
        "delay" -> "延时"
        "multi" -> "组合动作"
        else -> type
    }

    private fun actionSummary(action: DeckAction?): String {
        if (action == null) return "未绑定动作"
        val firstParam = action.params.values.firstOrNull() ?: ""
        return "${actionTypeName(action.type)} · $firstParam"
    }

    private fun DeckLayout.movePageButton(pageId: String, from: Int, to: Int): DeckLayout =
        copy(pages = pages.map { p ->
            if (p.id != pageId) p
            else {
                val list = p.buttons.toMutableList()
                val item = list.removeAt(from)
                list.add(to, item)
                p.copy(buttons = list)
            }
        })

    private fun parseHexColor(hex: String, fallback: Color): Color = try {
        Color(0xFF000000L or hex.removePrefix("#").toLong(16))
    } catch (e: Exception) {
        fallback
    }

    private fun defaultLayout(): DeckLayout = DeckLayout(
        columns = 4,
        pages = listOf(
            DeckPage(
                id = "common", name = "常用",
                buttons = listOf(
                    DeckButton(
                        "browser", "浏览器", "🌐", "#1565C0",
                        DeckAction("open_app", mapOf("name" to "Safari"))
                    ),
                    DeckButton(
                        "terminal", "终端", "💻", "#263238",
                        DeckAction("open_app", mapOf("name" to "Terminal"))
                    ),
                    DeckButton(
                        "finder", "访达", "📁", "#00838F",
                        DeckAction("open_app", mapOf("name" to "Finder"))
                    ),
                    DeckButton(
                        "screenshot", "截屏", "📸", "#6A1B9A",
                        DeckAction("shell", mapOf("cmd" to "screencapture -x ~/Desktop/deck_\$(date +%H%M%S).png"))
                    ),
                    DeckButton(
                        "vol_up", "音量+", "🔊", "#2E7D32",
                        DeckAction("applescript", mapOf("script" to "set volume output volume ((output volume of (get volume settings)) + 10)"))
                    ),
                    DeckButton(
                        "vol_down", "音量-", "🔉", "#2E7D32",
                        DeckAction("applescript", mapOf("script" to "set volume output volume ((output volume of (get volume settings)) - 10)"))
                    ),
                    DeckButton(
                        "mute", "静音", "🔇", "#C62828",
                        DeckAction("applescript", mapOf("script" to "set volume output muted (not (output muted of (get volume settings)))"))
                    ),
                    DeckButton(
                        "playpause", "播放/暂停", "⏯", "#EF6C00",
                        DeckAction("applescript", mapOf("script" to "tell application \"Music\" to playpause"))
                    )
                )
            ),
            DeckPage(
                id = "system", name = "系统",
                buttons = listOf(
                    DeckButton(
                        "lock", "锁屏", "🔒", "#455A64",
                        DeckAction("hotkey", mapOf("keys" to "cmd+ctrl+q"))
                    ),
                    DeckButton(
                        "launchpad", "启动台", "🚀", "#1565C0",
                        DeckAction("open_app", mapOf("name" to "Launchpad"))
                    ),
                    DeckButton(
                        "android_studio", "AS", "🤖", "#2E7D32",
                        DeckAction("open_app", mapOf("name" to "Android Studio"))
                    ),
                    DeckButton(
                        "sleep_display", "熄屏", "🌙", "#263238",
                        DeckAction("shell", mapOf("cmd" to "pmset displaysleepnow"))
                    )
                )
            )
        )
    )
}
