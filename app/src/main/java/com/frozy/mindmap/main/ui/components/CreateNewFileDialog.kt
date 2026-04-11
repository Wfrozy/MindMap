package com.frozy.mindmap.main.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.frozy.mindmap.R
import com.frozy.mindmap.main.models.MapListEntry
import com.frozy.mindmap.storage.models.StorageOption
import com.frozy.mindmap.storage.utils.checkIfFileNameIsInvalid
import com.frozy.mindmap.ui.theme.MindMapTypography

@Composable
fun CreateNewFileDialog(
    entry: MapListEntry,
    onTextFieldValueChange: (MapListEntry) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    currentSelectedStorage: StorageOption,
    onStorageOptionChange: (StorageOption) -> Unit
){
    var showAppStorageInfo by remember { mutableStateOf(value = false) }
    var showDeviceStorageInfo by remember { mutableStateOf(value = false) }
    var isFileNameInvalid by remember { mutableStateOf(value = false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.create_new_file_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = entry.name,
                    onValueChange = { newValue ->
                        if(!newValue.checkIfFileNameIsInvalid(blankCheck = false)){
                            isFileNameInvalid = newValue.checkIfFileNameIsInvalid()
                            onTextFieldValueChange(entry.copy(name = newValue))
                        }
                    },
                    label = {
                        Text(text = stringResource(id = R.string.create_new_file_name))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = isFileNameInvalid
                )
                Spacer(modifier = Modifier.height(height = 12.dp))
                Text(
                    text = stringResource(id = R.string.create_new_file_storage_option_subtitle),
                    style = MindMapTypography.titleMedium
                )
                Column {

                    //device storage option -----------------------

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        //device
                        RadioButton(
                            selected = currentSelectedStorage == StorageOption.DEVICE,
                            onClick = { onStorageOptionChange( StorageOption.DEVICE ) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = StorageOption.DEVICE.label))
                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.contentDescription_device_storage_info_icon),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showDeviceStorageInfo = !showDeviceStorageInfo }
                        )
                    }

                    if (showDeviceStorageInfo) {
                        Text(
                            text = stringResource(id = StorageOption.DEVICE.description),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    //app storage option -----------------------

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        //app
                        RadioButton(
                            selected = currentSelectedStorage == StorageOption.APP,
                            onClick = { onStorageOptionChange(StorageOption.APP) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = StorageOption.APP.label))
                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.contentDescription_app_storage_info_icon),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showAppStorageInfo = !showAppStorageInfo }
                        )
                    }

                    if (showAppStorageInfo) {
                        Text(
                            text = stringResource(id = StorageOption.APP.description),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isFileNameInvalid
            ) {
                Text(text = stringResource(id = R.string.word_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.word_cancel))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
}
