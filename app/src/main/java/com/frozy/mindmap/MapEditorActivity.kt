package com.frozy.mindmap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Construction
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frozy.mindmap.ui.theme.MindMapTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.ui.theme.MindMapShapes
import com.frozy.mindmap.ui.theme.MindMapTypography

@Composable
@Preview
fun MapEditorUIPreview(){
    MapEditorUI(
        backButtonOnClick = {},
        fileNameFromIntent = "Preview"
    )
}


class MapEditorActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileNameFromIntent = intent.getStringExtra("file_name") ?: "Unknown.json"
        val storageOptionFromIntent = intent.getStringExtra("storage")?.let { StorageOption.valueOf(it) } ?: StorageOption.DEVICE
        enableEdgeToEdge()
        setContent {
            MindMapTheme {
               MapEditorUI(
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
    backButtonOnClick: () -> Unit,
    fileNameFromIntent: String
){
    var isBottomSheetVisible by remember { mutableStateOf(value = false) }
    val fileNameFromIntentNoJson = fileNameFromIntent.removeSuffix(suffix = ".json")
    val currentActivity = LocalActivity.current
    DisposableEffect(isBottomSheetVisible) {
        currentActivity?.hideSystemStatusBar()
        onDispose { currentActivity?.hideSystemStatusBar() }
    }

    if (isBottomSheetVisible){
        ModalBottomSheet(
            onDismissRequest = { isBottomSheetVisible = false }
        ) {
            Column(
                modifier = Modifier.padding(all = 16.dp)
            ){
                EditorBottomSheetItem(
                    icon = Icons.Default.Lightbulb,
                    text = stringResource(R.string.map_editor_new_idea),
                    //todo
                    itemOnClick = { }
                )
                EditorBottomSheetItem(
                    icon = Icons.Default.Cable,
                    text = stringResource(R.string.map_editor_new_flow_idea),
                    //todo
                    itemOnClick = { }
                )
                EditorBottomSheetItem(
                    icon = Icons.Default.AddPhotoAlternate,
                    text = stringResource(R.string.map_editor_add_image),
                    includeSpacer = false,
                    //todo
                    itemOnClick = { }
                )
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
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
                }

            )
        },
        floatingActionButton = {
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
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.scale(scale = 1.5f)
                ) {
                    val commonColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
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