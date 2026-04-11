package com.frozy.mindmap.mapeditor.space.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frozy.mindmap.R

@Composable
fun NodeTextEditDialog(
    initialText: String,
    initialFontSize: TextUnit,
    onConfirm: (String, TextUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var nodeText by remember { mutableStateOf(value = initialText) }
    var fontSizeText by remember { mutableStateOf(value = initialFontSize.value.toInt().toString()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.node_text_edit_dialog_edit_text_textfield_label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                OutlinedTextField(
                    value = nodeText,
                    onValueChange = { nodeText = it },
                    label = { Text(text = stringResource(id = R.string.node_text_edit_dialog_node_text_textfield_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = fontSizeText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) {
                            fontSizeText = input
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.node_text_edit_dialog_font_size_textfield_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fontSize = fontSizeText.toIntOrNull()?.coerceIn(8, 200)
                        ?: initialFontSize.value.toInt()
                    onConfirm(nodeText, fontSize.sp)
                }
            ) {
                Text(text = stringResource(id = R.string.word_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.word_cancel))
            }
        }
    )
}