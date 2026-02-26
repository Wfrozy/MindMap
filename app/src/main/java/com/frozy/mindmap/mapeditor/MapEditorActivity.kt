package com.frozy.mindmap.mapeditor

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.ui.components.BottomSheetItem
import com.frozy.mindmap.mapeditor.note.ui.NoteScreen
import com.frozy.mindmap.R
import com.frozy.mindmap.mapeditor.model.MapItem
import com.frozy.mindmap.mapeditor.model.MapItemObject
import com.frozy.mindmap.mapeditor.space.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.ui.SpaceScreen
import com.frozy.mindmap.ui.util.hideSystemStatusBar
import com.frozy.mindmap.ui.theme.MindMapTheme
import com.frozy.mindmap.ui.theme.MindMapTypography
import kotlinx.coroutines.launch
import kotlin.collections.plus

//todo [small] add cool transition between main activity and this activity
//todo [small] make it so the system bars don't pop up whe you switch apps
//todo [small] something with the top app bar to make it more immersive
//todo [medium] add toggle for "editor mode" and "reader mode"
//todo [medium] add animations everywhere
//todo [small, optional] change boundary arrow to a "no you can't do this" symbol when you can't scroll or the fade color
class MapEditorActivity : ComponentActivity() {
    private val mapEditorVM: MapEditorViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileNameFromIntent = intent.getStringExtra("file_name") ?: "Unknown.json"
//        val storageOptionFromIntent = intent.getStringExtra("storage")?.let { StorageOption.valueOf(it) } ?: StorageOption.DEVICE
        enableEdgeToEdge()
        setContent {
            MindMapTheme {
               MapEditorUI(
                   mevm = mapEditorVM,
                   backButtonOnClick = { finish() },
                   fileNameFromIntent = fileNameFromIntent
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
    fileNameFromIntent: String
){
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var isBottomSheetVisible by remember { mutableStateOf(value = false) }

    val fileNameFromIntentNoJson = fileNameFromIntent.removeSuffix(suffix = ".json")
    val currentActivity = LocalActivity.current
    val isEditorModeEnabled by mevm.isEditorModeEnabled.collectAsState()

    val mapItemPagerList by mevm.mapItemPagerList.collectAsState()

    //default SpaceNode parameters when creating a node in a Space
    val defaultNodeWidth = 300f
    val defaultNodeHeight = 150f
    val defaultNodeBorderColor = MaterialTheme.colorScheme.surfaceContainer
    val defaultBackgroundColor = MaterialTheme.colorScheme.background
    val defaultNodeText = "Type here"
    val defaultNodeFontSize = MaterialTheme.typography.bodyMedium.fontSize

    val pagerState = rememberPagerState(pageCount = { mapItemPagerList.size })
    var isHorizontalPagerVisible by remember { mutableStateOf(value = false) }


    BackHandler(enabled = isEditorModeEnabled) {
        mevm.changeEditorModeState(value = false)
    }

    LaunchedEffect(mapItemPagerList.size) {
        if (mapItemPagerList.isNotEmpty()){
            pagerState.animateScrollToPage(mapItemPagerList.lastIndex)
            isHorizontalPagerVisible = true
        } else isHorizontalPagerVisible = false
    }
    DisposableEffect(isBottomSheetVisible) {
        currentActivity?.hideSystemStatusBar()
        onDispose { currentActivity?.hideSystemStatusBar() }
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
                        mevm.miplAddMapItem(mapItem = MapItem.Note())
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isBottomSheetVisible = false
                        }
                    }
                )
                BottomSheetItem(
                    icon = Icons.Default.Cable,
                    text = stringResource(R.string.map_editor_new_space),
                    itemOnClick = {
                        mevm.miplAddMapItem(mapItem = MapItem.Space())
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isBottomSheetVisible = false
                        }
                    }
                )
                BottomSheetItem(
                    icon = Icons.Default.AddPhotoAlternate,
                    text = stringResource(id = R.string.map_editor_add_image),
                    includeSpacer = false,
                    //todo [BIG] import image
                    itemOnClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isBottomSheetVisible = false
                        }
                    }
                )
            }
        }
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
                                    contentDescription = stringResource(R.string.contentDescription_back_button)
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
                                text = fileNameFromIntentNoJson,
                                style = MindMapTypography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            //todo [small] what could this even do?
                            onClick = {},
                            content = {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.contentDescription_more_map_options_in_map_editor)
                                )
                            }
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            Column {
                if(!isEditorModeEnabled){
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                sheetState.show()
                            }.invokeOnCompletion {
                                isBottomSheetVisible = true
                            }
                            currentActivity?.hideSystemStatusBar()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = stringResource(id = R.string.contentDescription_add_new_content_in_map_editor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(height = 8.dp))

                FloatingActionButton(
                    onClick = {
                        mevm.changeEditorModeState(value = !isEditorModeEnabled)
                        currentActivity?.hideSystemStatusBar()
                    }
                ) {
                    Icon(
                        imageVector = when (isEditorModeEnabled) {
                            false -> Icons.Default.Edit
                            true -> Icons.Default.EditOff
                        },
                        contentDescription = when (isEditorModeEnabled) {
                            false -> stringResource(R.string.contentDescription_enable_reader_mode)
                            true -> stringResource(R.string.contentDescription_enable_editor_mode)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(paddingValues = innerPadding)
        ) {
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
                            when (val currentPage = mapItemPagerList[i]) {
                                is MapItem.Note -> {
                                    NoteScreen(
                                        activity = currentActivity,
                                        note = currentPage,
                                        mevm = mevm,
                                        pagerList = mapItemPagerList
                                    )
                                }
                                is MapItem.Space -> {
                                    SpaceScreen(
                                        activity = currentActivity,
                                        nodes = currentPage.spaceNodeInfo,
                                        pagerState = pagerState,
                                        onAddNode = { canvasSize, camera ->
                                            val screenCenter = Offset(
                                                x = canvasSize.width / 2f,
                                                y = canvasSize.height / 2f
                                            )

                                            val defaultNodeOffset = (screenCenter - camera.offset) / camera.scale

                                            val defaultNode = MapItemObject.SpaceNode(
                                                offset = defaultNodeOffset,
                                                width = defaultNodeWidth,
                                                height = defaultNodeHeight,
                                                borderColor = defaultNodeBorderColor,
                                                backgroundColor = defaultBackgroundColor,
                                                text = defaultNodeText,
                                                fontSize = defaultNodeFontSize
                                            )

                                            mevm.miplAddSpaceNodeToSpace(
                                                mapItemUUID = currentPage.uuid,
                                                node = defaultNode
                                            )
                                        }
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
                                        .padding(4.dp)
                                        .size(8.dp)
                                        .background(color, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
            if(!isHorizontalPagerVisible) {
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
                                text = stringResource(R.string.background_text_in_empty_map),
                                color = commonColor,
                                style = MindMapTypography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}