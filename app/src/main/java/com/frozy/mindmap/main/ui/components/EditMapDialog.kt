package com.frozy.mindmap.main.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.frozy.mindmap.R
import com.frozy.mindmap.main.models.MapListEntry
import com.frozy.mindmap.storage.utils.checkIfFileNameIsInvalid

@Composable
fun EditMapDialog(
    currentMapEntry: MapListEntry,
    onDismiss: () -> Unit,
    onDelete:  () -> Unit,
    onConfirm: (MapListEntry) -> Unit
){
    var isFileNameInvalid by remember { mutableStateOf(value = false) }
    var entry by remember { mutableStateOf(value = currentMapEntry) }
    val originalMapName by remember { mutableStateOf(value = entry.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit map settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = entry.name,
                    onValueChange = { newValue ->
                        if(!newValue.checkIfFileNameIsInvalid(blankCheck = false)){
                            isFileNameInvalid = newValue.checkIfFileNameIsInvalid()
                            entry = entry.copy(name = newValue)
                        }
                    },
                    label = {
                        Text(text = stringResource(id = R.string.create_new_file_name))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = isFileNameInvalid
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(entry) },
                enabled = !isFileNameInvalid && originalMapName != entry.name
            ) {
                Text(text = stringResource(R.string.text_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.text_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.text_cancel))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
}