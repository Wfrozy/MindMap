package com.frozy.mindmap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.BugReport
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frozy.mindmap.ui.theme.MindMapTheme
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.MapEditorViewModel.MapItem
import com.frozy.mindmap.ui.theme.MindMapShapes
import com.frozy.mindmap.ui.theme.MindMapTypography

//todo add cool transition between main activity and this activity
//todo make it so the system bars don't pop up whe you switch apps
//todo something with the top app bar to make it more immersive
//todo add toggle for "editor mode" and "reader mode"
//todo make the text field in the text idea thing expand to the number of lines
//todo add animations everywhere
//todo add sfx to buttons and stuff maybe?
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

const val TEXTFIELD_MAX_LINES = 127

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapEditorUI(
    mevm: MapEditorViewModel,
    backButtonOnClick: () -> Unit,
    fileNameFromIntent: String
){
    var isBottomSheetVisible by remember { mutableStateOf(value = false) }
    val fileNameFromIntentNoJson = fileNameFromIntent.removeSuffix(suffix = ".json")
    val currentActivity = LocalActivity.current
    val isEditorModeEnabled by mevm.isEditorModeEnabled.collectAsState()
    val pagerList by mevm.pagerList.collectAsState()
    val pagerState = rememberPagerState(pageCount = { pagerList.size })
    var isHorizontalPagerVisible by remember { mutableStateOf(value = false) }

    BackHandler(enabled = isEditorModeEnabled) {
        mevm.changeEditorModeState(value = false)
    }

    LaunchedEffect(pagerList.size) {
        if (pagerList.isNotEmpty()){
            pagerState.animateScrollToPage(pagerList.lastIndex)
            isHorizontalPagerVisible = true
        } else isHorizontalPagerVisible = false
    }
    DisposableEffect(isBottomSheetVisible) {
        currentActivity?.hideSystemStatusBar()
        onDispose { currentActivity?.hideSystemStatusBar() }
    }

    AnimatedVisibility(
        visible = isBottomSheetVisible
    ) {
        ModalBottomSheet(
            onDismissRequest = { isBottomSheetVisible = false }
        ) {
            Column(
                modifier = Modifier.padding(all = 16.dp)
            ) {
                EditorBottomSheetItem(
                    icon = Icons.Default.Lightbulb,
                    text = stringResource(R.string.map_editor_new_note),
                    //todo
                    itemOnClick = {
                        mevm.changePagerList(value = pagerList + MapItem.Note.create())
                        isBottomSheetVisible = false
                    }
                )
                EditorBottomSheetItem(
                    icon = Icons.Default.Cable,
                    text = stringResource(R.string.map_editor_new_space),
                    //todo
                    itemOnClick = {
                        mevm.changePagerList(value = pagerList + MapItem.Space())
                        isBottomSheetVisible = false
                    }
                )
                EditorBottomSheetItem(
                    icon = Icons.Default.AddPhotoAlternate,
                    text = stringResource(R.string.map_editor_add_image),
                    includeSpacer = false,
                    //todo
                    itemOnClick = {
                        isBottomSheetVisible = false
                    }
                )
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
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
                            //todo
                            onClick = {},
                            content = {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.contentDescription_more_map_options_in_map_editor)
                                )
                            }
                        )
                        //testing
                        IconButton(
                            onClick = { mevm.changeEditorModeState(value = !isEditorModeEnabled) },
                            content = {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    tint = Color(0xFF23D715),
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
                FloatingActionButton(
                    onClick = {
                        isBottomSheetVisible = true
                        currentActivity?.hideSystemStatusBar()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = stringResource(R.string.contentDescription_add_new_content_in_map_editor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            key = { listIndex -> pagerList[listIndex].uuid }
                        ) { listIndex ->
                            when (val currentPage = pagerList[listIndex]) {
                                is MapItem.Note -> {
                                    NoteScreen(
                                        activity = currentActivity,
                                        note = currentPage,
                                        mevm = mevm,
                                        pagerList = pagerList
                                    )
                                }
                                is MapItem.Space -> {
                                    SpaceScreen(
                                        activity = currentActivity,
                                        nodes = currentPage.nodeInfo
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
//
//@Composable
//fun SpaceContent(
//    nodes: List<MapEditorViewModel.SpaceNode>,
//    camera: MapEditorViewModel.SpaceCameraState
//) {
//    nodes.forEach { node ->
//        Box(
//            modifier = Modifier
//                .size(150.dp)
//                .background(Color(0xFF2A2A2A))
//        ) {
//            Text(
//                text = node.text,
//                color = Color.White,
//                modifier = Modifier.padding(8.dp)
//            )
//        }
//    }
//}
//
//@Composable
//fun DottedBackground(
//    modifier: Modifier = Modifier,
//    dotColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
//    dotRadius: Dp = 1.5.dp,
//    spacing: Dp = 24.dp
//) {
//    Canvas(modifier = modifier) {
//        val radiusPx = dotRadius.toPx()
//        val spacingPx = spacing.toPx()
//
//        val cols = (size.width / spacingPx).toInt() + 1
//        val rows = (size.height / spacingPx).toInt() + 1
//
//        for (x in 0..cols) {
//            for (y in 0..rows) {
//                drawCircle(
//                    color = dotColor,
//                    radius = radiusPx,
//                    center = Offset(
//                        x * spacingPx,
//                        y * spacingPx
//                    )
//                )
//            }
//        }
//    }
//}