package com.frozy.mindmap.main

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.frozy.mindmap.ui.components.CreateNewFileDialog
import com.frozy.mindmap.ui.components.EditMapDialog
import com.frozy.mindmap.ui.components.BottomSheetItem
import com.frozy.mindmap.ui.components.ExtraConfirmationDialog
import com.frozy.mindmap.R
import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION
import com.frozy.mindmap.main.models.MapListEntry
import com.frozy.mindmap.storage.models.StorageOption
import com.frozy.mindmap.storage.utils.sanitizeAndEnsureExtension
import com.frozy.mindmap.ui.theme.MindMapShapes
import com.frozy.mindmap.ui.theme.MindMapTheme
import com.frozy.mindmap.ui.theme.MindMapTypography
import com.frozy.mindmap.ui.utils.openSelectedMap
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val mainActivityVM: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindMapTheme {
                MainActivityUI(mavm = mainActivityVM)
            }
        }
    }
}

//avoids buggy mess
@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityUI(mavm: MainActivityViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val defaultName = stringResource(id = R.string.default_map_name_with_extension)
    val defaultNameNoExtension = stringResource(id = R.string.default_map_name_no_extension)

    var pendingMapEntry by remember {
        //random values
        mutableStateOf(value = MapListEntry(
            uuid = UUID.randomUUID(),
            name = defaultNameNoExtension,
            storedIn = StorageOption.APP,
            lastModified = 0
        ))
    }

    var selectedStorageRadioOption by remember { mutableStateOf(value = StorageOption.DEVICE) }

    //the list of files that gets shown on screen
    val mapEntryList by mavm.mapEntryList.collectAsState()

    var mapEntryBeingEdited by remember { mutableStateOf<MapListEntry?>(value = null) }

    var isCreateDialogVisible by remember { mutableStateOf(value = false) }
    var isDeleteConfirmationVisible by remember { mutableStateOf(value = false) }
    var isBottomSheetVisible by remember { mutableStateOf(value = false) }
    val sheetState = rememberModalBottomSheetState()


    val toastFileSavingCancelled = stringResource(id = R.string.toast_file_saving_cancelled)
    val toastFilePickerCancelled = stringResource(id = R.string.toast_file_picker_cancelled)

    LaunchedEffect(key1 = Unit) {
        mavm.toastEvents.collect { event ->
            val message =
                if (event.formatArgs != null) {
                    context.getString(
                        event.messageResId,
                        //spread operator omg :O
                        *event.formatArgs.toTypedArray()
                    )
                } else {
                    context.getString(event.messageResId)
                }
            Toast.makeText(
                context,
                message,
                event.toastLength
            ).show()
        }
    }

    val createMapInDeviceStorageLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument(mimeType = "*/*")
    ) { uri ->

        if(uri == null) {
            Toast.makeText(context, toastFileSavingCancelled, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        try {
            //make uris accessible even after app restarts. Also saves them to disk
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            Log.w("URI_PERMISSION", "Map creation SecurityException from $uri", e)
        }


        pendingMapEntry.name.let { mapName ->
            if(mapName.isNotEmpty()){
                mavm.createMapInDeviceStorage(mapName, defaultName, uri)
            }
        }
    }

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        //OpenMultipleDocuments returns an empty list when it is cancelled
        if(uris.isEmpty()){
            Toast.makeText(context, toastFilePickerCancelled, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        uris.forEach { uri ->
            try {
                //make uris accessible even after app restarts. Also saves them to disk
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w("URI_PERMISSION", "File import SecurityException from $uri", e)
            }
        }

        mavm.importMaps(userSelectedUris = uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
//                    IconButton(
//                        onClick = { context.openSettingsActivity() },
//                        content = {
//                            Icon(
//                                imageVector = Icons.Default.Settings,
//                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
//                                contentDescription = stringResource(id = R.string.contentDescription_settings_iconButton)
//                            )
//                        }
//                    )
                },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        sheetState.show()
                    }.invokeOnCompletion {
                        isBottomSheetVisible = true
                    } }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.contentDescription_create_new_file_fab)
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            color = MaterialTheme.colorScheme.background
        ){
            Column {
                Text(
                    text = stringResource(id = R.string.main_activity_select_map_header_text),
                    style = MindMapTypography.titleLarge,
                    modifier = Modifier.padding(all = 16.dp)
                )
                if(mapEntryList.isEmpty()){
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
                                    text = stringResource(id = R.string.background_text_in_empty_map),
                                    color = commonColor,
                                    style = MindMapTypography.labelLarge
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        items(
                            items = mapEntryList.sortedByDescending { entry ->
                                entry.lastModified
                            },
                            key = { it.uuid }
                        ) { entry ->
                            val borderColor = when {
                                entry.storedIn == StorageOption.APP -> lerp(
                                    start = MaterialTheme.colorScheme.tertiary,
                                    stop = Color.Blue,
                                    fraction = 0.5f
                                )
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Row(
                                modifier = Modifier
                                    .border(
                                        border = BorderStroke(
                                            width = 2.dp,
                                            color = borderColor
                                        ),
                                        shape = MindMapShapes.medium
                                    )
                                    .fillParentMaxWidth()
                                    .padding(all = 10.dp)
                                    .clickable(onClick = {
                                        val metadata = mavm.resolveMetadata(entryUUID = entry.uuid)
                                        openSelectedMap(context, metadata)
                                    })
                            ) {
                                val mapIconHeight = 56.dp
                                Column {
                                    Icon(
                                        painter = painterResource(id = R.drawable.vecteezy_map),
                                        contentDescription = null,
                                        modifier = Modifier.size(
                                            width = 56.dp,
                                            height = mapIconHeight
                                        )
                                    )
                                }
                                Column {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(
                                            start = 8.dp,
                                            top = 8.dp,
                                            bottom = 0.dp,
                                            end = 8.dp
                                        )
                                    )
                                    Text(
                                        text = "Map | ${stringResource(id = entry.storedIn.label)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(
                                            start = 8.dp,
                                            top = 2.dp,
                                            bottom = 2.dp
                                        ),
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.weight(weight = 1F))

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier
                                        .height(height = mapIconHeight)
                                        .padding(all = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(id = R.string.contentDescription_settings_for_selected_map_icon),
                                        modifier = Modifier
                                            .clickable { mapEntryBeingEdited = entry }
                                            .size(size = 28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(height = 8.dp))
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
                            icon = Icons.Default.Add,
                            text = stringResource(id = R.string.create_new_file_title),
                            itemOnClick = { isCreateDialogVisible = true }
                        )
                        BottomSheetItem(
                            icon = Icons.Default.ImportExport,
                            text = stringResource(id = R.string.import_file),
                            itemOnClick = {
                                importFilesLauncher.launch(input = arrayOf("*/*"))
                                coroutineScope.launch {
                                    sheetState.hide()
                                }.invokeOnCompletion {
                                    isBottomSheetVisible = false
                                }
                            },
                            includeSpacer = false
                        )
                    }
                }
            }
        }
    }

    if (isCreateDialogVisible) {
        CreateNewFileDialog(
            entry = pendingMapEntry,
            onTextFieldValueChange = { pendingMapEntry = it },
            onDismiss = { isCreateDialogVisible = false },
            onConfirm = {
                if (selectedStorageRadioOption == StorageOption.APP) {
                    mavm.createMapInAppStorage(mapName = pendingMapEntry.name, defaultName)
                } else {
                    //launches the system create UI with a suggested filename
                    createMapInDeviceStorageLauncher.launch(input = pendingMapEntry.name.sanitizeAndEnsureExtension())
                }
                isCreateDialogVisible = false
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    isBottomSheetVisible = false
                }
            },
            currentSelectedStorage = selectedStorageRadioOption,
            onStorageOptionChange = { option -> selectedStorageRadioOption = option }
        )
    }

    mapEntryBeingEdited?.let {
        EditMapDialog(
            onDismiss = { mapEntryBeingEdited = null },
            onConfirm = { updatedEntry ->
                if (it.storedIn == StorageOption.APP){
                    mavm.renameMapInAppStorage(
                        newName = updatedEntry.name,
                        entryUUID = it.uuid
                    )
                } else {
                    mavm.renameMapInDeviceStorage(
                        newName = updatedEntry.name,
                        entryUUID = it.uuid
                    )
                }
                mapEntryBeingEdited = null
            },
            onDelete = { isDeleteConfirmationVisible = true },
            currentMapEntry = it
        )
        if(isDeleteConfirmationVisible){
            if(it.storedIn == StorageOption.APP){
                ExtraConfirmationDialog(
                    onDismiss = { isDeleteConfirmationVisible = false },
                    text = {
                        Text(text = stringResource(id = R.string.file_deletion_confirmation_message))
                    },
                    onConfirm = {
                        mavm.deleteMapInAppStorage(entryUUID = mapEntryBeingEdited!!.uuid)
                        isDeleteConfirmationVisible = false
                        mapEntryBeingEdited = null
                    }
                )
            } else {
                ExtraConfirmationDialog(
                    onDismiss = { isDeleteConfirmationVisible = false },
                    text = {
                        Text(text = stringResource(id = R.string.file_deletion_consequence_message))
                    },
                    onConfirm = {
                        mavm.deleteMapInDeviceStorage(
                            entryUUID = mapEntryBeingEdited!!.uuid,
                            mapName = mapEntryBeingEdited!!.name
                        )
                        isDeleteConfirmationVisible = false
                        mapEntryBeingEdited = null
                    }
                )
            }
        }
    }
}