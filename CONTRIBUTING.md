# Contributing to Team Deck | 参与贡献 Team Deck

English | [中文](#中文)

Thank you for your interest in contributing to Team Deck! We welcome contributions of all kinds, especially new plugins that expand the capabilities of this platform.

## Plugin Development

Team Deck is built on a flexible plugin architecture using Compose Multiplatform. A single plugin can provide UI for both the Android client and the Desktop host.

### Getting Started

1.  **Understand the Interface**: All plugins must implement the `IPlugin` interface located in the `shared` module.
2.  **Define UI**: Implement `AppUI()` for the mobile side and `DesktopUI()` for the desktop side.
3.  **Handle Logic**: Use `onTrigger` to handle actions sent from the mobile device to the desktop.
4.  **Data Sync**: Use the `messageSender` provided by the host to send data back and forth.

### Sample Plugin Structure
```kotlin
class MyCustomPlugin : IPlugin {
    override val id = "my.plugin.unique.id"
    override val name = "My Plugin"

    @Composable
    override fun AppUI() {
        // UI for Android
    }

    @Composable
    override fun DesktopUI() {
        // UI for Desktop
    }

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        // Logic execution
    }
}
```

### Building and Testing
Currently, plugins are bundled as APK files or loaded dynamically within the project modules. To add a new plugin:
1. Create a new module following the pattern of `systemmonitorplugin`.
2. Register your plugin in the `PluginLoader`.

## Sharing Your Work
We encourage you to:
- **Fork** the repository and add your plugin module.
- Submit a **Pull Request** to integrate your plugin into the main project.
- Share your ideas in the Issues section!

**Important**: By contributing to Team Deck, you agree that your contributions will be licensed under the **CC BY-NC-SA 4.0** license.

---

<a name="中文"></a>

# 参与贡献 Team Deck

感谢您对 Team Deck 项目的关注！我们欢迎各种形式的贡献，特别是能扩展平台功能的各种新插件。

## 插件开发

Team Deck 基于 Compose Multiplatform 构建，拥有灵活的插件架构。单个插件即可同时为 Android 客户端和 Desktop 宿主端提供 UI。

### 快速上手

1.  **理解接口**：所有插件必须实现位于 `shared` 模块中的 `IPlugin` 接口。
2.  **定义 UI**：实现 `AppUI()`（手机端）和 `DesktopUI()`（电脑端）。
3.  **处理逻辑**：使用 `onTrigger` 处理从手机发送到电脑的动作指令。
4.  **数据同步**：利用宿主注入的 `messageSender` 实现跨端数据通信。

### 示例插件结构
```kotlin
class MyCustomPlugin : IPlugin {
    override val id = "my.plugin.unique.id"
    override val name = "我的插件"

    @Composable
    override fun AppUI() {
        // Android 端 UI
    }

    @Composable
    override fun DesktopUI() {
        // Desktop 端 UI
    }

    override fun onTrigger(actionId: String, params: Map<String, String>) {
        // 业务逻辑执行
    }
}
```

### 构建与测试
目前插件可以作为独立模块构建。添加新插件的步骤：
1. 参考 `systemmonitorplugin` 创建新模块。
2. 在 `PluginLoader` 中注册您的插件。

## 分享您的成果
我们鼓励您：
- **Fork** 本仓库并添加您的插件模块。
- 提交 **Pull Request** 将您的插件集成到主项目中。
- 在 Issues 区分享您的创意和建议！

**重要提示**：向 Team Deck 贡献代码即表示您同意您的贡献将基于 **CC BY-NC-SA 4.0** 协议发布。
