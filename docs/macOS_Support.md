# macOS 支持说明

Team_Deck 桌面端最初以 Windows 为主要目标，本文记录 macOS 适配情况、构建要求与权限配置。

## 构建要求

- **JDK 17**（必须）：Gradle 8.2.1 最高支持 JDK 20 运行，Android Studio 自带的 JBR 21 无法运行本项目的 Gradle；系统 JDK 11 低于 AGP 8.0 的要求。
  - 命令行构建：`export JAVA_HOME=~/Library/Java/JavaVirtualMachines/corretto-17.jdk/Contents/Home`
  - Android Studio 打开本项目：`Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK` 选择 JDK 17（如 corretto-17）。
- `local.properties` 指向 Android SDK：`sdk.dir=/Users/<you>/Library/Android/sdk`
- 常用命令：
  - 桌面端运行：`./gradlew :desktopApp:run`
  - 双端同启（需连接手机）：`./gradlew runAll`
  - 插件打包：`./gradlew :deckplugin:assembleDebug :androidstudioplugin:assembleDebug`

## 权限配置（重要）

**辅助功能授权**：Deck 控制台 / AS 控制台的「快捷键注入」（`java.awt.Robot`）与「菜单点击」（AppleScript System Events）需要在
`系统设置 → 隐私与安全性 → 辅助功能` 中授权运行桌面端的进程：

- 通过 `./gradlew :desktopApp:run` 启动时，授权对象是对应的 `java`（JDK 17 的 java 可执行文件）；
- 打包成 .app（`./gradlew :desktopApp:packageDmg`）后，授权对象是打包出的应用本体。

未授权时快捷键类按键会执行失败（结果条会提示），adb 类按键不受影响。

**AppleScript 控制系统事件**：首次触发菜单点击时 macOS 可能弹出「想要控制 System Events」的确认框，允许即可。

## 已修复的 macOS 平台问题

| 问题 | 位置 | 修复 |
|---|---|---|
| `Runtime.exec` 单字符串带引号导致 argv[0] 含字面引号，adb reverse 在 macOS 上全部静默失败（ENOENT） | `desktopApp/.../utils/AdbUtils.kt` | 改为 `exec(arrayOf(...))` 数组形式 |
| adb 探测路径缺少 Apple Silicon Homebrew 位置 | 同上 | 增加 `/opt/homebrew/bin/adb` |
| shared 硬编码 `compose.desktop.windows_x64`，macOS 构建拉取 Windows skiko 产物 | `shared/build.gradle.kts` | 改为 `compose.desktop.currentOs` |
| WebSocket 服务只入队一条 101 响应，手机断开后重连握手永久挂起 | `desktopApp/.../utils/NsdManagerUtils.kt` | 改用自定义 Dispatcher，每个握手请求都返回 101 |

## 已知遗留（不影响使用）

- 插件持久化目录沿用 Windows 风格路径 `~/AppData/Local/TeamDeck`（macOS 上同样可用，刻意保留以兼容既有安装）。
- Deck 配置文件位于 `~/AppData/Roaming/TeamDeck/deck_layout.json`。
- IDE 控制类按键的 keymap 按平台自动切换（macOS：Run=⌃R / Debug=⌃D / Stop=⌘F2 / Build=⌘F9；Windows：Shift+F10 等）；「Sync Gradle」按键依赖 AppleScript 菜单点击，仅 macOS 可用。
