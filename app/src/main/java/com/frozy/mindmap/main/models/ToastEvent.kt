package com.frozy.mindmap.main.models

import android.widget.Toast

data class ToastEvent (
    val messageResId: Int,
    val formatArgs: List<String>? = null,
    val toastLength: Int = Toast.LENGTH_LONG
)
