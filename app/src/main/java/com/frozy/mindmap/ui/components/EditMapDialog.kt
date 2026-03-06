package com.frozy.mindmap.ui.components

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
import com.frozy.mindmap.storage.FileData
import com.frozy.mindmap.storage.utils.checkIfFileNameIsInvalid

@Composable
fun EditMapDialog(
    currentFileData: FileData,
    onDismiss: () -> Unit,
    onDelete:  () -> Unit,
    onConfirm: (FileData) -> Unit,
    fileListIndex: Int
){
    var isFileNameInvalid by remember { mutableStateOf(value = false) }
    var fileData by remember { mutableStateOf(value = currentFileData) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit map settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = fileData.fileNameNoJson,
                    onValueChange = { newValue ->
                        if(!newValue.checkIfFileNameIsInvalid(blankCheck = false)){
                            isFileNameInvalid = newValue.checkIfFileNameIsInvalid()
                            fileData = fileData.copy(fileName = newValue)
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
                onClick = { onConfirm(fileData) },
                enabled = !isFileNameInvalid
            ) {
                Text(text = stringResource(R.string.edit_map_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.edit_map_dialog_delete_button),
                    color = MaterialTheme.colorScheme.error
                )
            }
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.create_new_file_cancel_button))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
}