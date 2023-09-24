# 实现可控宽带的侧边抽屉
### 实现效果
正常:\
![正常](doc/Screenshot_20230907_210143.png "正常样子")
![打开抽屉后](doc/Screenshot_20230907_204456.png "拉开后")
### 起因：
需要一个侧滑抽屉，但是官方material3中提供的不能控制drawer的宽度，因此只能自己实现，但又发现material3中官方不提供
Swipeable的api,从2022年就有用户给google官方提issue，但至今还没有结果。看官方文档介绍说material2中依然提供，所以我们需要进行结合使用


### 实现代码

```kotlin
//ExtendNavigationDrawer.kt
/**
 *@描述：自定义抽屉类，完善抽屉类别，添加可控宽度
 *@time：2023/9/7
 *@author:zhangzexin
 */

/**
 * Possible values of [DrawerState].
 */
enum class DrawerValue {
    /**
     * The state of the drawer when it is closed.
     */
    Closed,

    /**
     * The state of the drawer when it is open.
     */
    Open
}

/**
 * State of the [CustomizeNavigationDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@Stable
class DrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
) {

    internal val swipeableState = ExtendSwipeableState(
        initialValue = initialValue,
        animationSpec = AnimationSpec,
        confirmStateChange = confirmStateChange
    )

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    @OptIn(ExperimentalMaterialApi::class)
    val currentValue: DrawerValue
        get() {
            return swipeableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    @OptIn(ExperimentalMaterialApi::class)
    val isAnimationRunning: Boolean
        get() {
            return swipeableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() = animateTo(DrawerValue.Open, AnimationSpec)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = animateTo(DrawerValue.Closed, AnimationSpec)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @OptIn(ExperimentalMaterialApi::class)
    suspend fun animateTo(targetValue: DrawerValue, anim: AnimationSpec<Float>) {
        swipeableState.animateTo(targetValue, anim)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    @OptIn(ExperimentalMaterialApi::class)
    suspend fun snapTo(targetValue: DrawerValue) {
        swipeableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    @OptIn(ExperimentalMaterialApi::class)
    val targetValue: DrawerValue
        get() = swipeableState.targetValue

    /**
     * The current position (in pixels) of the drawer container.
     */
    @OptIn(ExperimentalMaterialApi::class)
    val offset: State<Float>
        get() = swipeableState.offset

    companion object {
        /**
         * The default [Saver] implementation for [DrawerState].
         */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) }
            )
    }
}

/**
 * Create and [remember] a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }
}


/**
 * <a href="https://m3.material.io/components/navigation-drawer/overview" class="external" target="_blank">Material Design navigation drawer</a>.
 *
 * Navigation drawers provide ergonomic access to destinations in an app. They’re often next to
 * app content and affect the screen’s layout grid.
 *
 * ![Navigation drawer image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * Dismissible standard drawers can be used for layouts that prioritize content (such as a
 * photo gallery) or for apps where users are unlikely to switch destinations often. They should
 * use a visible navigation menu icon to open and close the drawer.
 *
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param withsize drawer Width size
 * @param drawerVelocityThreshold The threshold (in dp per second) that the end velocity has to exceed in order to animate to the next state, even if the positional thresholds have not been reached.
 * @param content content of the rest of the UI
 */
var SemanticsPropertyReceiver.paneTitle by SemanticsProperties.PaneTitle

@ExperimentalMaterialApi
@Composable
fun CustomizeNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    drawerVelocityThreshold: Dp = 400.dp,
    withsize: Dp,
    content: @Composable () -> Unit
) {
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    val navigationMenu = resources.getString(androidx.compose.ui.R.string.navigation_menu)
    val drawerWidthPx = with(LocalDensity.current) { withsize.toPx() }
    val minValue = -drawerWidthPx
    val maxValue = 0f
    val scope = rememberCoroutineScope()
    val anchors = mapOf(minValue to DrawerValue.Closed, maxValue to DrawerValue.Open)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(

        modifier
            .width(100.dp)
            .swipeable(
                state = drawerState.swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
                reverseDirection = isRtl,
                velocityThreshold = drawerVelocityThreshold,
                resistance = null
            )
    ) {
        Layout(content = {

            Box(Modifier.semantics {
                paneTitle = navigationMenu
                if (drawerState.isOpen) {
                    dismiss {
                        if (
                            drawerState.swipeableState
                                .confirmStateChange(DrawerValue.Closed)
                        ) {
                            scope.launch { drawerState.close() }
                        }; true
                    }
                }
            }) {
                drawerContent()
            }
            Box {
                content()
            }
        }) { measurables, constraints ->
            val sheetPlaceable = measurables[0].measure(constraints)
            val contentPlaceable = measurables[1].measure(constraints)
            layout(contentPlaceable.width, contentPlaceable.height) {
                contentPlaceable.placeRelative(
                    sheetPlaceable.width + drawerState.offset.value.roundToInt(),
                    0
                )
                sheetPlaceable.placeRelative(drawerState.offset.value.roundToInt(), 0)
            }
        }
    }
}

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)
```
```kotlin
//ExtendSwipeableState.kt
@OptIn(ExperimentalMaterialApi::class)
class ExtendSwipeableState<T>(
    initialValue: T,
    internal val animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    internal val confirmStateChange: (newValue: T) -> Boolean = { true }
): SwipeableState<T>(initialValue,animationSpec,confirmStateChange) {

}
```
#### 布局：
```kotlin
// TeamDeckApp.kt
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TeamDeckApp() {
    // icons to mimic drawer destinations
    val items = listOf(
        Icons.Default.Favorite,
        Icons.Default.Face,
        Icons.Default.Email
    )
    val selectedItem = remember { mutableStateOf(items[0]) }
    AppTheme {
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        CustomizeNavigationDrawer(
            drawerState= drawerState,
            modifier = Modifier.fillMaxWidth(),
            withsize = 200.dp,
            drawerContent = {
                ModalDrawerSheet(Modifier.width(200.dp)) {
                    Spacer(Modifier.height(12.dp))
                    items.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item, contentDescription = null) },
                            label = { Text(item.name) },
                            selected = item == selectedItem.value,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                                selectedItem.value = item
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }


            }) {
            ...
        }
    }
}
```
SwipeableState中的参数由于internal进行修饰，只能在同模块下可见，所以需要ExtendSwipeableState继承来规避，否则无法调用confirmStateChange，但是这并不意味着我们这样做是正确的,由于大部分api都是实验性的，所以需要注意后续更新
