package com.frozy.mindmap.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

fun Color.lighten(fraction: Float): Color {
    return lerp(
        start = this,
        stop = Color.White,
        fraction = fraction
    )
}