package com.frozy.mindmap.ui.util

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Activity.hideSystemStatusBar(){
    WindowCompat.setDecorFitsSystemWindows(this.window, false)

    val controller = WindowInsetsControllerCompat(this.window, this.window.decorView)

    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}

fun Activity.showSystemStatusBar(){
    WindowCompat.setDecorFitsSystemWindows(this.window, false)

    val controller = WindowInsetsControllerCompat(this.window, this.window.decorView)

    controller.show(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}