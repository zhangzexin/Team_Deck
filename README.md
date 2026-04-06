# Team Deck

English | [中文](#中文)

Team Deck is a cross-platform, professional-grade interaction tool built with **Compose Multiplatform**. It allows you to use your Android device as a powerful "Stream Deck-like" remote controller for your Desktop, enabling seamless scene switching, performance monitoring, and script execution.

![Team Deck Banner](doc/int.png)

## 🚀 Key Features

-   **Automatic Device Discovery**: Uses **mDNS (NSD)** to automatically find and connect to your Desktop within the same local network.
-   **Real-time Interaction**: High-performance interaction via **WebSockets**, ensuring low-latency communication between mobile and desktop.
-   **Multi-Platform Support**: Ready to run on **Android** (Client) and **JVM/Desktop** (Host).
-   **Plugin Architecture**: 
    -   Write once, run everywhere: A single plugin implements UI for both mobile and desktop.
    -   Supports dynamic data interaction and hidden complexity via the `IPlugin` interface.
-   **System Performance Monitoring**: Built-in support for monitoring CPU/GPU usage and other system metrics.
-   **Script Execution**: Trigger complex workflows (Python, C++, Java scripts) directly from your phone.
-   **Advanced UI Elements**:
    -   Custom Navigation Drawer with controllable width.
    -   Native-feel Drag & Drop support for plugin installation on Desktop.

## 📂 Project Structure

-   `androidApp`: Android mobile application logic and resources.
-   `desktopApp`: Desktop host application for Windows/macOS/Linux.
-   `shared`: Core multiplatform logic including the plugin system and networking.
-   `docs/`: Technical deep-dives and troubleshooting guides.

## 🛠️ Getting Started

### Prerequisites

-   **JDK 17** or higher
-   **Android SDK** (for the mobile app)
-   **ADB** (for debugging and auto-launch)

### Quick Run

You can launch both the Android client and the Desktop host simultaneously using the following Gradle task:

```bash
./gradlew runAll
```
*Note: Ensure your Android device is connected via ADB and has debugging enabled.*

## 🧩 Plugin Development

We provide a specialized framework for plugin development. You can create custom modules that define their own UI and logic. Refer to [CONTRIBUTING.md](CONTRIBUTING.md) for a detailed guide on how to build and share your own plugins.

## 📄 License | 项目协议

This project is licensed under the **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)** License. 

-   **NonCommercial**: You may not use the material for commercial purposes.
-   **Attribution**: You must give appropriate credit.
-   **ShareAlike**: If you remix, transform, or build upon the material, you must distribute your contributions under the same license.

See [LICENSE.txt](LICENSE.txt) for the full legal text.

---

<a name="中文"></a>

# Team Deck

[English](#team-deck) | 中文

Team Deck 是一款基于 **Compose Multiplatform** 构建的跨平台专业交互工具。它允许您将 Android 设备转变为电脑端的强大“中控台”（类似于 Stream Deck），实现无缝的场景切换、性能监测和脚本执行。

![Team Deck Banner](doc/int.png)

## 🚀 核心功能

-   **设备自动发现**：利用 **mDNS (NSD)** 技术，在同一局域网内自动搜索并连接您的电脑。
-   **实时交互**：通过高性能 **WebSocket** 实现超低延迟的移动端与桌面端通信。
-   **多平台支持**：原生支持 **Android**（客户端）和 **JVM/Desktop**（宿主端）。
-   **插件化系统架构**：
    -   “一套代码，两端运行”：单个插件即可实现手机与电脑两端的 UI 交互。
    -   基于 `IPlugin` 接口的标准化数据同步。
-   **系统性能监控**：内置插件，可实时监控 CPU/GPU 使用率及其他系统指标。
-   **脚本执行**：直接通过手机触发复杂的自动化工作流（支持 Python, C++, Java 等）。
-   **高级 UI 特性**：
    -   支持宽度可控的自定义侧边抽屉。
    -   桌面端支持原生级的文件拖拽加载插件功能。

## 📂 目录结构

-   `androidApp`：安卓端 App 代码及逻辑。
-   `desktopApp`：桌面端应用逻辑及资源文件。
-   `shared`：核心多平台逻辑，包含插件系统及网络协议。
-   `docs/`：技术细节深入探讨及故障排除指南。

## 🛠️ 快速上手

### 环境要求

-   **JDK 17** 或更高版本
-   **Android SDK**（用于构建手机端）
-   **ADB**（用于调试及自动唤起应用）

### 一键运行

您可以使用以下 Gradle 任务同时启动 Android 客户端和桌面宿主端：

```bash
./gradlew runAll
```
*注意：请确保手机已通过 ADB 连接，且已开启开发者选项。*

## 🧩 插件开发

我们提供了一套专门的插件开发框架。您可以创建自定义模块来定义专属的 UI 和逻辑。详细开发流程请参考 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 📄 项目协议

本项目采用 **知识共享 署名-非商业性使用-相同方式共享 4.0 国际 (CC BY-NC-SA 4.0)** 许可协议。

-   **非商业性使用**：您不得将本素材用于商业目的。
-   **署名**：您必须给出适当的署名。
-   **相同方式共享**：如果您再混合、转换、或者基于本素材进行创作，您必须基于与原先许可协议相同的许可协议分发您贡献的作品。

详情请参阅 [LICENSE.txt](LICENSE.txt) 完整法律文本。
