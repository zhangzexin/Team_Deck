package com.zzx.desktop.teamdeck.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zzx.desktop.teamdeck.md_theme_dark_outlineVariant

@Composable
fun ButtonLayout() {
    var number by remember {
        mutableStateOf(6)
    }
    var fix_w by remember {
        mutableStateOf(2)
    }
    Box(modifier = Modifier.size(1100.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = this@BoxWithConstraints.maxWidth - 20.dp
            val height = this@BoxWithConstraints.maxHeight - 20.dp
            val fix_h = number / fix_w
//                    val baseButton = if (fix_h < fix_w) height/fix_w else width/fix_h
            val baseButton = 180.dp
            val h = (height / baseButton).toInt()
            val w = (width / baseButton).toInt()
            val sw = (width - (baseButton * w)) / w
            val sh = (height - (baseButton * h)) / h
            ItemBuild(h * w, w, sw, sh, baseButton)
        }
    }
//    Row {
//        Button(onClick = {
//            setText("stop")
//            println("点击了")
//        }) {
//            Text(onText())
//        }
//        Button(onClick = {
//        }) {
//            Text(onIndex().toString())
//        }
//    }

}

@Composable
private fun ItemBuild(n: Int, w: Int, sw: Dp, sh: Dp, baseButton: Dp) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(w),
        horizontalArrangement = Arrangement.spacedBy(sw + 1.dp),
        verticalArrangement = Arrangement.spacedBy(sh + 1.dp),
        contentPadding = PaddingValues(vertical = sh, horizontal = sw),
        userScrollEnabled = false
    ) {
        items(n) {
            Card(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).size(baseButton, baseButton)
                    .clickable {
//                    Toast
//                        .makeText(context, "点击了$it", Toast.LENGTH_SHORT)
//                        .show()
//                        println("点击了")
                    }, backgroundColor = md_theme_dark_outlineVariant
            ) {

            }
        }
    }
}