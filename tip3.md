# 实现desktop端的拖住文件
## 文件拖拽两种方式
### 第一种方式参考：\
目前国内能查到的应该是《[使用ComposeDesktop开发一款桌面端多功能APK工具](https://juejin.cn/post/7122645579439538183)》\
[《Compose for Desktop桌面端简单的APK工具》](https://juejin.cn/post/7233951543115776055)\

但是目前有个问题:
```kotlin
 val target = object : DropTarget() {
                @Synchronized
                override fun drop(evt: DropTargetDropEvent) {
                    evt.acceptDrop(DnDConstants.ACTION_REFERENCE)
                    val droppedFiles = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    droppedFiles.first()?.let {
                        onFileDrop(it.toString())
                    }
                }
            }

window.contentPane.components[0].dropTarget = target
```
通过<font color=Red>window</font>添加到对应区域中的<font color=Red>Component</font>后，依然是全局，可能是版本问题造成无法像上面那两位老师一样实现。

### 第二种方式
第二种是在国外也有这样的需求，因此有人给官方提了issue，而且官方也刚好放出了对应新api,<font color=Red>Modifier.onExternalDrag</font>。\
  代码更简单了，看下面例子
```kotlin
@OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PluginsLoadWidget() {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    var isDragging by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(0.5f)
            .background(if (!isDragging) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.inverseOnSurface, shape = RoundedCornerShape(10f))
            .drawBehind {
                drawRoundRect(
                    topLeft = Offset(10f, 10f),
                    size = Size(drawContext.size.width - 20f, drawContext.size.height - 20f),
                    color = Color.Black,
                    style = stroke,
                    cornerRadius = CornerRadius(10f,10f)
                )
            }.onExternalDrag(
            onDragStart = {
                isDragging = true
            },
            onDragExit = {
                isDragging = false
            },
            onDrag = {

            },
            onDrop = { state ->
                val dragData = state.dragData
                if (dragData is DragData.Image) {
//                    println(dragData.readImage().toString())
                } else if (dragData is DragData.FilesList) {
                    dragData.readFiles().first().let {
                        println(it)
                    }
                }
                isDragging = false
            }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource("system_update_alt-24px.xml"),
            contentDescription = "上传插件",
            modifier = Modifier.fillMaxSize(1f)
        )
    }
}

```
Gif:
![](doc/upload.gif)

当然第二种方式，也是基于通过window添加<font color=Red>target</font>实现监听对应的file文件\
这次分享就到这！\
感谢您的观看
