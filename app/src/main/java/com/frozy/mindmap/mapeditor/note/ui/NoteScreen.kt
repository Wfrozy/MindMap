package com.frozy.mindmap.mapeditor.note.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.ui.utils.hideSystemStatusBar
import com.frozy.mindmap.mapeditor.MapEditorViewModel
import com.frozy.mindmap.mapeditor.model.MapItem
import com.frozy.mindmap.ui.utils.showSystemStatusBar
import java.util.UUID

//todo [medium] add more customization features to the text
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteScreen(
    activity: Activity?,
    mevm: MapEditorViewModel,
    mapItemUUID: UUID,
    pagerList: List<MapItem>
) {
    val mipl = mevm.mapItemPagerList.collectAsState()
    val thisNote by remember(key1 = mipl) {
        derivedStateOf {
            mipl.value.first { mapItem ->
                mapItem is MapItem.Note && mapItem.uuid == mapItemUUID
            } as MapItem.Note
        }
    }



    var isTextFieldFocused by remember { mutableStateOf(value = false) }
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var title by remember(thisNote.uuid) { mutableStateOf(thisNote.titleText) }
    var content by remember(thisNote.uuid) { mutableStateOf(thisNote.contentText) }

    BackHandler(enabled = isTextFieldFocused) {
        focusManager.clearFocus(force = true)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
    ) {
        item {
            TextField(
                value = title,
                placeholder = {
                    Text(text = stringResource(R.string.note_screen_title_placeholder))
                },
                textStyle = MaterialTheme.typography.titleLarge,
                onValueChange = {
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
                        mevm.miplChangeNoteTitle(
                            mapItemUUID = thisNote.uuid,
                            newTitle = title
                        )
                    },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
        }
        item {
            TextField(
                value = content,
                placeholder = {
                    Text(text = stringResource(R.string.note_screen_note_content_placeholder))
                },
                onValueChange = {
                    content = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight()
                    .onFocusChanged { focusState ->
                        isTextFieldFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            activity?.showSystemStatusBar()
                        } else {
                            activity?.hideSystemStatusBar()
                        }
                        mevm.miplChangeNoteContent(
                            mapItemUUID = thisNote.uuid,
                            newContent = content
                        )
                    },
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
}