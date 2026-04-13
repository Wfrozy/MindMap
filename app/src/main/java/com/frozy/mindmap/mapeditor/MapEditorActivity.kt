package com.frozy.mindmap.mapeditor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.note.ui.NoteScreen
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.ui.components.SpaceScreen
import com.frozy.mindmap.ui.components.BottomSheetItem
import com.frozy.mindmap.ui.theme.MindMapTheme
import com.frozy.mindmap.ui.theme.MindMapTypography
import com.frozy.mindmap.ui.utils.hideSystemStatusBar
import kotlinx.coroutines.launch
import java.util.UUID

class MapEditorActivity : ComponentActivity() {
    private val mevm: MapEditorViewModel by viewModels()

    private lateinit var entryUUIDFromIntent: UUID

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        mevm.saveMap(entryUUID = entryUUIDFromIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entryUUIDFromIntent = UUID.fromString(intent.getStringExtra("entryUUID"))
        val mapNameFromIntent = intent.getStringExtra("mapName")
        mevm.loadMap(entryUUID = entryUUIDFromIntent)
        enableEdgeToEdge()
        setContent {
            MindMapTheme {
               MapEditorUI(
                   mevm = mevm,
                   backButtonOnClick = { finish() },
                   displayNameTitle = mapNameFromIntent
               )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapEditorUI(
    mevm: MapEditorViewModel,
    backButtonOnClick: () -> Unit,
    displayNameTitle: String?
){
    val context = LocalContext.current
    val toastFailedToGetMapName = stringResource(id = R.string.toast_mapeditor_failed_to_get_map_name)
    val displayNameTitle =
        if(displayNameTitle == null) {
            Toast.makeText(context,
                toastFailedToGetMapName, Toast.LENGTH_LONG).show()
            stringResource(id = R.string.word_unknown)
        } else {
            displayNameTitle
        }
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var isBottomSheetVisible by remember { mutableStateOf(value = false) }

    val displayNameNoExtension = displayNameTitle
        .removeSuffix(suffix = APP_FILE_EXTENSION)
        .removeSuffix(suffix = ".json")
    val currentActivity = LocalActivity.current

    val isMapLoadingFinished by mevm.isMapLoadingFinished.collectAsState()
    val initialPageIndex by mevm.initialPageIndex.collectAsState()
    val isEditorModeEnabled by mevm.isEditorModeEnabled.collectAsState()
    val mapItemPagerList by mevm.mapItemPagerList.collectAsState()
    val allSelectedNodes by mevm.allSelectedNodes.collectAsState()

    val numberOfSelectedObjects by remember(key1 = allSelectedNodes) {
        derivedStateOf { allSelectedNodes.size }
    }

    val pagerState = rememberPagerState(pageCount = { mapItemPagerList.size })
    var isHorizontalPagerVisible by remember { mutableStateOf(value = mapItemPagerList.isNotEmpty()) }

    BackHandler(enabled = isEditorModeEnabled) {
        mevm.changeEditorModeState(value = false)
    }

    LaunchedEffect(key1 = pagerState.currentPage) {
        mevm.updateCurrentPageIndex(pagerState.currentPage)
    }

    LaunchedEffect(
        key1 = mapItemPagerList.size,
        key2 = isMapLoadingFinished
    ) {
        Log.v("", "LaunchedEffect isMapLoadingFinished: $isMapLoadingFinished")
        if (!isMapLoadingFinished) return@LaunchedEffect

        Log.v("", "LaunchedEffect mipl.size: ${mapItemPagerList.size}")
        if (mapItemPagerList.isNotEmpty()) {
            isHorizontalPagerVisible = true
            val targetPage =
                if (initialPageIndex < mapItemPagerList.size)
                    initialPageIndex
                else mapItemPagerList.lastIndex

            if(targetPage > 0) { pagerState.scrollToPage(targetPage) }
        } else {
            isHorizontalPagerVisible = false
        }
    }

    //avoids race conditions with the composable being toggled with if() and .show() animation
    LaunchedEffect(key1 = isBottomSheetVisible) {
        if (isBottomSheetVisible) {
            sheetState.show()
        }
    }


    DisposableEffect(key1 = isBottomSheetVisible) {
        currentActivity?.hideSystemStatusBar()
        onDispose { currentActivity?.hideSystemStatusBar() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(left = 0),
        topBar = {
            if (!isEditorModeEnabled) {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = backButtonOnClick,
                            content = {
                                Icon(
                                    imageVector = Icons.Default.ArrowCircleLeft,
                                    contentDescription = stringResource(id = R.string.contentDescription_back_button)
                                )
                            }
                        )
                    },
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayNameNoExtension,
                                style = MindMapTypography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                Log.v("", "mipl: $mapItemPagerList")
                                Log.v("", "initialPage: $initialPageIndex")
                                Log.v("", "isMapLoadingFinished: $isMapLoadingFinished")
                                Log.v("", "isHorizontalPagerVisible: $isHorizontalPagerVisible")
                            },
                            content = {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(id = R.string.contentDescription_more_map_options_in_map_editor),
                                    tint = Color.Transparent
                                )
                            }
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            Column {
                val currentMapItem = mapItemPagerList.getOrNull(index = pagerState.currentPage)

                if (currentMapItem is MapItem.Space) {
                    if (numberOfSelectedObjects == 1) {
                        FloatingActionButton(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                mevm.changeNodeEditorSheetVisibility(value = true)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(id = R.string.contentDescription_add_new_content_in_map_editor)
                            )
                        }
                        Spacer(modifier = Modifier.height(height = 8.dp))
                    }
                    FloatingActionButton(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            mevm.changeNodeSheetVisibility(value = true)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = stringResource(id = R.string.contentDescription_add_new_content_in_map_editor)
                        )
                    }
                    Spacer(modifier = Modifier.height(height = 8.dp))
                }
                FloatingActionButton(
                    onClick = {
                        isBottomSheetVisible = true
                        currentActivity?.hideSystemStatusBar()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.contentDescription_add_new_content_in_map_editor)
                    )
                }
            }
        }    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(paddingValues = innerPadding)
        ) {
            if(isMapLoadingFinished) {
                AnimatedVisibility(
                    visible = isHorizontalPagerVisible,
                    enter = fadeIn()
                ) {
                    Surface {
                        Box {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                pageSpacing = 69.dp,
                                beyondViewportPageCount = 1,
                                key = { listIndex -> mapItemPagerList[listIndex].uuid }
                            ) { i ->
                                //Warning: currentPage is NOT REACTIVE!!!!!!!!! (definitely did not waste hours on a bug)
                                when (val currentPage = mapItemPagerList[i]) {
                                    is MapItem.Note -> {
                                        NoteScreen(
                                            activity = currentActivity,
                                            mevm = mevm,
                                            mapItemUUID = currentPage.uuid
                                        )
                                    }
                                    is MapItem.Space -> {
                                        SpaceScreen(
                                            activity = currentActivity,
                                            mevm = mevm,
                                            mapItemUUID = currentPage.uuid,
                                            pagerState = pagerState,
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            ) {
                                repeat(times = pagerState.pageCount) { index ->
                                    val color =
                                        if (pagerState.currentPage == index) Color.White
                                        else Color.Gray

                                    Box(
                                        modifier = Modifier
                                            .padding(all = 4.dp)
                                            .size(size = 8.dp)
                                            .background(color, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if(isMapLoadingFinished && !isHorizontalPagerVisible) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.scale(scale = 1.5f)
                    ) {
                        val commonColor =
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icons8_sad_face),
                                tint = commonColor,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(id = R.string.background_text_in_empty_map),
                                color = commonColor,
                                style = MindMapTypography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
    if(isBottomSheetVisible) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    isBottomSheetVisible = false
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(all = 16.dp)
            ) {
                BottomSheetItem(
                    icon = Icons.Default.Lightbulb,
                    text = stringResource(id = R.string.map_editor_new_note),
                    itemOnClick = {
                        mevm.addMapItem(
                            mapItem = MapItem.Note(
                                titleText = "",
                                contentText = ""
                            )
                        )
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isBottomSheetVisible = false
                        }
                    }
                )

                Spacer(modifier = Modifier.height(height = 8.dp))

                BottomSheetItem(
                    icon = Icons.Default.Cable,
                    text = stringResource(id = R.string.map_editor_new_space),
                    itemOnClick = {
                        mevm.addMapItem(
                            mapItem = MapItem.Space(
                                cameraState = SpaceCameraState(),
                                objectInfo = emptyList()
                            )
                        )
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isBottomSheetVisible = false
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                BottomSheetItem(
                    icon = Icons.Default.Delete,
                    text = stringResource(id = R.string.bottom_sheet_item_delete_current_map_item),
                    itemOnClick = {
                        val currentIndex = pagerState.currentPage
                        val targetIndex = when {
                            mapItemPagerList.size <= 1 -> -1  // deleting last item
                            currentIndex > 0 -> currentIndex - 1
                            else -> 0
                        }

                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isBottomSheetVisible = false
                            if (targetIndex >= 0) {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(page = targetIndex)
                                }.invokeOnCompletion {
                                    mevm.deleteMapItem(
                                        mapItemUUID = mapItemPagerList[currentIndex].uuid
                                    )
                                }
                            } else {
                                mevm.deleteMapItem(
                                    mapItemUUID = mapItemPagerList[currentIndex].uuid
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}