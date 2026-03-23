package com.frozy.mindmap.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb

fun Color.lighten(fraction: Float): Color {
    return lerp(
        start = this,
        stop = Color.White,
        fraction = fraction
    )
}

fun Color.strengthen(
    saturationBoost: Float = 0.2f,
    valueBoost: Float = 0.1f
): Color {
    val hsv = FloatArray(size = 3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[1] = (hsv[1] + saturationBoost).coerceIn(0f, 1f) // saturation
    hsv[2] = (hsv[2] + valueBoost).coerceIn(0f, 1f) // brightness
    return Color(color = android.graphics.Color.HSVToColor(hsv))
}