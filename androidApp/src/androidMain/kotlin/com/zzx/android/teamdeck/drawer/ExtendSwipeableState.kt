package com.zzx.android.teamdeck.drawer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.SwipeableState

/**
 *@描述：
 *@time：2023/9/7
 *@author:zhangzexin
 */
@OptIn(ExperimentalMaterialApi::class)
class ExtendSwipeableState<T>(
    initialValue: T,
    internal val animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    internal val confirmStateChange: (newValue: T) -> Boolean = { true }
): SwipeableState<T>(initialValue,animationSpec,confirmStateChange) {

}