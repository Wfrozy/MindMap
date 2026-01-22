package com.frozy.mindmap

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.frozy.mindmap.MapEditorViewModel.MapItem
import com.frozy.mindmap.ui.theme.MindMapShapes
import com.frozy.mindmap.ui.theme.MindMapTypography

@Composable
fun EditMapDialog(
    currentFileData: FileData,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit map settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentFileData.fileName,
                    onValueChange = {},
                    label = {
                        Text(text = stringResource(id = R.string.create_new_file_name))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
//                    isError = isFileNameInvalid
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.edit_map_dialog_confirm_button))
            }
        },
        dismissButton = {
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

@Composable
fun CreateNewFileDialog(
    currentFileData: FileData,
    onTextFieldValueChangeSetter: (FileData) -> Unit,
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
                    value = currentFileData.fileName,
                    onValueChange = { newValue ->
                        isFileNameInvalid = checkIfFileNameIsInvalid(string = newValue)
                        onTextFieldValueChangeSetter(currentFileData.copy(fileName = newValue))
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
                Text(text = stringResource(id = R.string.create_new_file_confirm_button))
            }
        },
        dismissButton = {
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

@Composable
fun EditorBottomSheetItem(
    icon: ImageVector,
    contentDescription: String? = null,
    text: String,
    itemOnClick: () -> Unit,
    includeSpacer: Boolean = true
){
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MindMapShapes.medium,
        modifier = Modifier
            .clickable(onClick = { itemOnClick() })
            .fillMaxWidth()
    ) {
        Row {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
            )
            Text(
                text= text,
                modifier = Modifier.padding(all = 8.dp)
            )
        }
    }
    if(includeSpacer){ Spacer(modifier = Modifier.height(height = 8.dp)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteScreen(
    activity: Activity?,
    note: MapItem.Note,
    mevm: MapEditorViewModel,
    pagerList: List<MapItem>
) {
    var isTextFieldFocused by remember { mutableStateOf(value = false) }
    val focusManager = LocalFocusManager.current
    var title by remember(note.uuid) { mutableStateOf(note.titleText) }
    var content by remember(note.uuid) { mutableStateOf(note.contentText) }

    BackHandler(enabled = isTextFieldFocused) {
        focusManager.clearFocus(force = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = title,
            textStyle = MaterialTheme.typography.titleLarge,
            onValueChange = { //the last time u were coding this u left off here changing the onValueChange stuff, good luck
                title = it
            },
            singleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isTextFieldFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        activity?.showSystemStatusBar()
                    } else {
                        activity?.hideSystemStatusBar()
                    }
                    mevm.changeNoteTitle(
                        noteUUID = note.uuid,
                        newTitle = title
                    )
                },
//            colors = TextFieldDefaults.colors(
//                focusedContainerColor = MaterialTheme.colorScheme.background,
//                unfocusedContainerColor = MaterialTheme.colorScheme.background
//            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = content,
            onValueChange = {
                content = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onFocusChanged { focusState ->
                    isTextFieldFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        activity?.showSystemStatusBar()
                    } else {
                        activity?.hideSystemStatusBar()
                    }
                    mevm.changeNoteContent(
                        noteUUID = note.uuid,
                        newContent = content
                    )
                },
            maxLines = TEXTFIELD_MAX_LINES,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                errorIndicatorColor = MaterialTheme.colorScheme.background,
                focusedIndicatorColor = MaterialTheme.colorScheme.background,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                disabledIndicatorColor = MaterialTheme.colorScheme.background
            )
        )
    }
}
@Composable
fun SpaceScreen(
    activity: Activity?,
    nodes: List<MapEditorViewModel.SpaceNode>
){
    var camera by remember { mutableStateOf(value = MapEditorViewModel.SpaceCameraState()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    camera = camera.copy(
                        //todo formula
                        offset = camera.offset + pan,
                        scale = (camera.scale * zoom).coerceIn(0.5f, 4f)
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = camera.offset.x
                    translationY = camera.offset.y
                    scaleX = camera.scale
                    scaleY = camera.scale
                }
        ) {
            drawCircle(
                color = Color.Red,
                radius = 20f,
                center = Offset(x = 0f, y = 0f)
            )
        }
    }
}