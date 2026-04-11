package com.frozy.mindmap.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.frozy.mindmap.R

@Composable
fun ExtraConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    text: @Composable (() -> Unit)? = null
){
    AlertDialog(
        title = { Text(text = "Delete map?") },
        text = text,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(text = stringResource(id = R.string.word_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.word_cancel))
            }
        },
    )
}