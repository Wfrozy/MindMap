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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION
import com.frozy.mindmap.mapeditor.note.ui.NoteScreen
import com.frozy.mindmap.mapeditor.space.constants.DefaultNodeValues.DEFAULT_NODE_HEIGHT
import com.frozy.mindmap.mapeditor.space.constants.DefaultNodeValues.DEFAULT_NODE_WIDTH
import com.frozy.mindmap.mapeditor.space.models.MapItem
import com.frozy.mindmap.mapeditor.space.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.ui.components.SpaceScreen
import com.frozy.mindmap.ui.components.BottomSheetItem
import com.frozy.mindmap.ui.theme.MindMapTheme
import com.frozy.mindmap.ui.theme.MindMapTypography
import com.frozy.mindmap.ui.utils.hideSystemStatusBar
import com.frozy.mindmap.ui.utils.lighten
import kotlinx.coroutines.launch
import java.util.UUID

//todo [BIG] images!

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
    val displayNameTitle = if(displayNameTitle == null) {
        Toast.makeText(context, "Failed to get map name.", Toast.LENGTH_LONG).show()
        "Unknown"
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
    val numberOfSelectedNodes by remember(key1 = allSelectedNodes) {
        derivedStateOf { allSelectedNodes.size }
    }

    //default SpaceNode parameters when creating a node in a Space
    val defaultNodeWidth = DEFAULT_NODE_WIDTH
    val defaultNodeHeight = DEFAULT_NODE_HEIGHT
    val defaultNodeBorderColor = Color.White
    val defaultNodeBackgroundColor = MaterialTheme.colorScheme.background.lighten(fraction = 0.12f)
    val defaultNodeText = stringResource(id = R.string.default_space_node_text)
    val defaultNodeFontSize = MaterialTheme.typography.bodyMedium.fontSize

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
                if (pagerState.pageCount != 0) {
                    if (mapItemPagerList[pagerState.currentPage] is MapItem.Space) {
                        if(numberOfSelectedNodes == 1) {
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
                                imageVector = Icons.Default.Cable,
                                contentDescription = stringResource(id = R.string.contentDescription_add_new_content_in_map_editor)
                            )
                        }
                        Spacer(modifier = Modifier.height(height = 8.dp))
                    }
                }
                FloatingActionButton(
                    onClick = {
                        isBottomSheetVisible = true
                        currentActivity?.hideSystemStatusBar()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = stringResource(id = R.string.contentDescription_add_new_content_in_map_editor)
                    )
                }
            }
        }
    ) { innerPadding ->
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
                                            mapItemUUID = currentPage.uuid,
                                            pagerList = mapItemPagerList
                                        )
                                    }
                                    is MapItem.Space -> {
                                        SpaceScreen(
                                            activity = currentActivity,
                                            mevm = mevm,
                                            mapItemUUID = currentPage.uuid,
                                            pagerState = pagerState,
                                            onAddNode = { canvasSize, camera, _ ->
                                                val mapItemUUID = currentPage.uuid

                                                val canvasCenterX = (canvasSize.width/2f - camera.offset.x) / camera.scale
                                                val canvasCenterY = (canvasSize.height/2f - camera.offset.y) / camera.scale

                                                val defaultNodeOffset = Offset(
                                                    x = canvasCenterX - defaultNodeWidth/2,
                                                    y = canvasCenterY - defaultNodeHeight/2
                                                )

                                                val defaultNode = MapItemObject.SpaceNode(
                                                    offset = defaultNodeOffset,
                                                    width = defaultNodeWidth,
                                                    height = defaultNodeHeight,
                                                    borderColor = defaultNodeBorderColor,
                                                    backgroundColor = defaultNodeBackgroundColor,
                                                    text = defaultNodeText,
                                                    fontSize = defaultNodeFontSize
                                                )

                                                mevm.miplAddSpaceNodeToSpace(
                                                    mapItemUUID = mapItemUUID,
                                                    node = defaultNode
                                                )
                                            },
                                            onNodeHit = { _, nodeUUID ->
                                                mevm.miplSelectSpaceNode(
                                                    mapItemUUID = currentPage.uuid,
                                                    nodeUUID = nodeUUID,
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
                        mevm.miplAddMapItem(mapItem = MapItem.Note())
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
                        mevm.miplAddMapItem(mapItem = MapItem.Space())
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
                        mevm.miplRemoveMapItem(mapItemUUID = mapItemPagerList[pagerState.currentPage].uuid)
                    }
                )
            }
        }
    }
}