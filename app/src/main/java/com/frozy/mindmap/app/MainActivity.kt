package com.frozy.mindmap.app

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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.frozy.mindmap.ui.components.CreateNewFileDialog
import com.frozy.mindmap.ui.components.EditMapDialog
import com.frozy.mindmap.ui.components.BottomSheetItem
import com.frozy.mindmap.ui.components.ExtraConfirmationDialog
import com.frozy.mindmap.storage.FileData
import com.frozy.mindmap.storage.FileIO
import com.frozy.mindmap.R
import com.frozy.mindmap.storage.StorageOption
import com.frozy.mindmap.storage.util.applyBasicContent
import com.frozy.mindmap.storage.util.sanitizeAndEnsureJsonExtension
import com.frozy.mindmap.ui.theme.MindMapShapes
import com.frozy.mindmap.ui.theme.MindMapTheme
import com.frozy.mindmap.ui.theme.MindMapTypography
import com.frozy.mindmap.ui.util.openSelectedMap
import com.frozy.mindmap.ui.util.openSettingsActivity
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

//todo [BIG] put the file list in a DataStore to make it so imported maps don't disappear
//todo [medium, bonus points] mime type stuff so that there is .mindmap
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityUI(
    mavm: MainActivityViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentFileData by remember { mutableStateOf(value = FileData()) }
    var selectedStorage by remember { mutableStateOf(value = StorageOption.DEVICE) }

    //the list of files that gets shown on screen
    val fileList by mavm.fileList.collectAsState()

    var fileBeingEdited by remember { mutableStateOf<FileData?>(value = null) }

    var isCreateDialogVisible by remember { mutableStateOf(value = false) }

    var isBottomSheetVisible by remember { mutableStateOf(value = false) }
    val sheetState = rememberModalBottomSheetState()

    var isDeleteConfirmationVisible by remember { mutableStateOf(value = false) }

    //this variable exists because i can't use context.getString() anymore inside sanitizeAndEnsureJsonExtension()
    val fallbackString = stringResource(id = R.string.default_map_name_with_json)
    val fallbackStringNoJson = stringResource(id = R.string.default_map_name_no_json)

    //this variable depends on currentFileData so this syntax is needed
    var sanitizedFileName by remember(currentFileData.fileName) {
        mutableStateOf(value = currentFileData.fileName.sanitizeAndEnsureJsonExtension(fallbackString = fallbackString))
    }
    var sanitizedFileNameNoJson by remember(sanitizedFileName) {
        mutableStateOf(value = sanitizedFileName.removeSuffix(suffix = ".json"))
    }

    //these variables exist because i can't use context.getString() anymore inside the toasts
    val toastFileCreatedSuccess = stringResource(id = R.string.toast_file_created_success, sanitizedFileNameNoJson)
    val toastFileCreatedFail = stringResource(id = R.string.toast_file_created_fail, sanitizedFileNameNoJson)
    val toastFileSavingCancelled = stringResource(id = R.string.toast_file_saving_cancelled)
    val toastFileLoadingCancelled = stringResource(id = R.string.toast_file_picker_cancelled)
    val toastFileLoadingJSONException = stringResource(R.string.toast_file_picker_json_exception)
    val toastFileLoadingSecurityException = stringResource(R.string.toast_file_picker_security_exception)
    val toastFileLoadingFileNotFoundException = stringResource(R.string.toast_file_picker_file_not_found_exception)
    val toastFileLoadingIOException = stringResource(R.string.toast_file_picker_io_exception)

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument(mimeType = "application/json")
    ) { uri: Uri? ->

        if(uri == null) {
            Toast.makeText(context, toastFileSavingCancelled, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        //puts these parameters from the current fileData into the json
        val jsonObj = JSONObject().applyBasicContent(
            fileName = sanitizedFileName,
            selectedStorage = selectedStorage,
            fileContent = currentFileData,
        )
        //transforms the json object to a string and indents it
        val jsonText = jsonObj.toString(2)

        //make it run separately from the UI thread
        coroutineScope.launch {
            val isWriteSuccessful = FileIO.writeTextToUri(context, uri, jsonText)
            if (isWriteSuccessful) {
                //add the created file to the list
                mavm.changeFileList(value = fileList + currentFileData.copy(
                    fileName = sanitizedFileNameNoJson,
                    storedIn = selectedStorage,
                    timeStampID = System.currentTimeMillis(),
                    uri = uri
                ))
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.w(
                        "createDocumentLauncher takePersistableUriPermission",
                        "Failed to persist permission for $uri",
                        e
                    )
                }

                Toast.makeText(context, toastFileCreatedSuccess, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, toastFileCreatedFail, Toast.LENGTH_LONG).show()
            }
        }
    }

    val openMultipleDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        //OpenMultipleDocuments returns an empty list when it is cancelled
        if(uris.isEmpty()){
            Toast.makeText(context, toastFileLoadingCancelled, Toast.LENGTH_SHORT).show()
        }
        coroutineScope.launch {
            val operationResultList = FileIO.getJsonDataFromUris(
                context = context,
                uris = uris
            )
            operationResultList.forEachIndexed { index, opRes ->
                if (opRes.isSuccess) {
                    mavm.changeFileList(
                        value = fileList + FileData(
                            fileName = opRes.data?.first ?: fallbackStringNoJson,
                            fileContent = opRes.data?.second ?: JSONObject(),
                            uri = uris[index]
                        )
                    )
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uris[index],
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        //todo [medium]
                    } catch (e: SecurityException) {
                        Log.w(
                            "openMultipleDocumentsLauncher takePersistableUriPermission",
                            "Failed to persist permission for ${uris[index]}",
                            e
                        )
                    }
                } else {
                    when(opRes.errorInfo){
                        is JSONException -> {
                            Toast.makeText(context, toastFileLoadingJSONException, Toast.LENGTH_LONG).show()
                        }
                        is SecurityException -> {
                            Toast.makeText(context, toastFileLoadingSecurityException, Toast.LENGTH_LONG).show()
                        }
                        is FileNotFoundException -> {
                            Toast.makeText(context, toastFileLoadingFileNotFoundException, Toast.LENGTH_LONG).show()
                        }
                        is IOException -> {
                            Toast.makeText(context, toastFileLoadingIOException, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
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
                    IconButton(
                        onClick = { context.openSettingsActivity() },
                        content = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = stringResource(R.string.contentDescription_settings_iconButton)
                            )
                        }
                    )
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
                    contentDescription = stringResource(R.string.contentDescription_create_new_file_fab)
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ){
            Column {
                Text(
                    text = "Select map:",
                    style = MindMapTypography.titleLarge,
                    modifier = Modifier.padding(all = 16.dp)
                )
                if(fileList.isEmpty()){
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
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        items(
                            items = fileList,
                            key = { it.timeStampID }
                        ) { file ->
                            val borderColor = when {
                                file.storedIn == StorageOption.APP -> MaterialTheme.colorScheme.inverseOnSurface
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
                                    .clickable(onClick = { openSelectedMap(context, file) })
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
                                        text = file.fileName.removeSuffix(suffix = ".json"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(
                                            start = 8.dp,
                                            top = 8.dp,
                                            bottom = 0.dp,
                                            end = 8.dp
                                        )
                                    )
                                    Text(
                                        text = "Map | ${stringResource(id = file.storedIn.label)}",
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
                                        contentDescription = stringResource(R.string.contentDescription_settings_for_selected_map_icon),
                                        modifier = Modifier
                                            .clickable { fileBeingEdited = file }
                                            .size(28.dp)
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
                                openMultipleDocumentsLauncher.launch(arrayOf("application/json"))
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
            currentFileData = currentFileData,
            onTextFieldValueChangeSetter = { currentFileData = it },
            onDismiss = { isCreateDialogVisible = false },
            onConfirm = {
                if (selectedStorage == StorageOption.DEVICE) {
                    //launches the system create UI with a suggested filename
                    createDocumentLauncher.launch(input = sanitizedFileName)
                }
                //if storedIn.APP
                else {
                    coroutineScope.launch {
                        val uniqueFileName = FileIO.makeFileNameUnique(
                            dir = context.filesDir,
                            baseName = sanitizedFileName
                        )
                        val jsonObj = JSONObject().applyBasicContent(
                            fileName = sanitizedFileName,
                            selectedStorage = selectedStorage,
                            fileContent = currentFileData
                        )
                        val jsonText = jsonObj.toString(2)

                        val isWriteSuccessful =
                            FileIO.writeTextToFileInAppStorage(context, uniqueFileName, jsonText)
                        if (isWriteSuccessful) {
                            val newFileData = currentFileData.copy(
                                storedIn = StorageOption.APP,
                                fileName = uniqueFileName,
                                timeStampID = System.currentTimeMillis(),
                                filePath = File(context.filesDir, uniqueFileName)
                            )
                            mavm.changeFileList(value = fileList + newFileData)
                            //todo [very small] string resource here
                            Toast.makeText(
                                context,
                                "Saved ${uniqueFileName.removeSuffix(suffix = ".json")}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to save $sanitizedFileNameNoJson",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                isCreateDialogVisible = false
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    isBottomSheetVisible = false
                }
            },
            currentSelectedStorage = selectedStorage,
            onStorageOptionChange = { option -> selectedStorage = option }
        )
    }
    //todo [BIG] finish this, this changes the fileList and not the actual name of the files so they will lose changes when loaded from disk
    fileBeingEdited?.let { file ->
        val fileIndex = fileList.indexOf(file)
        val toastFileDeletionSuccess = stringResource(R.string.toast_file_deleted_fail, file.fileNameNoJson)
        val toastFileDeletionFail = stringResource(R.string.toast_file_deleted_success, file.fileNameNoJson)

        EditMapDialog(
            onDismiss = { fileBeingEdited = null },
            onConfirm = { updatedFile ->
                val mutableList = fileList.toMutableList()
                mutableList[fileIndex] = updatedFile
                mavm.changeFileList(value = mutableList)

                fileBeingEdited = null
            },
            onDelete = { isDeleteConfirmationVisible = true },
            currentFileData = file,
            fileListIndex = fileIndex
        )
        if(isDeleteConfirmationVisible){
            if(file.storedIn == StorageOption.APP){
                ExtraConfirmationDialog(
                    onDismiss = { isDeleteConfirmationVisible = false },
                    text = {
                        Text(text = stringResource(R.string.file_deletion_confirmation_message))
                    },
                    onConfirm = {
                        //for app storage
                        coroutineScope.launch {
                            if (FileIO.deleteFileInAppStorage(
                                    context = context,
                                    fileName = file.fileName
                                )
                            ) {
                                Toast.makeText(
                                    context,
                                    toastFileDeletionSuccess,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(context, toastFileDeletionFail, Toast.LENGTH_LONG)
                                    .show()
                            }
                            mavm.initLoadFiles()
                            isDeleteConfirmationVisible = false
                            fileBeingEdited = null
                        }
                    }
                )

            } else {
                ExtraConfirmationDialog(
                    onDismiss = { isDeleteConfirmationVisible = false },
                    text = {
                        Text(text = stringResource(R.string.file_deletion_consequence_message))
                    },
                    //for device storage
                    onConfirm = {
                        coroutineScope.launch {
                            if (FileIO.deleteFileInDeviceStorage(context, file.uri)) {
                                Toast.makeText(
                                    context,
                                    toastFileDeletionSuccess,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(context, toastFileDeletionFail, Toast.LENGTH_LONG)
                                    .show()
                            }
                            //todo [BIG] the files disappear when this gets run because it doesn't take into account the device storage
                            mavm.initLoadFiles()
                            isDeleteConfirmationVisible = false
                            fileBeingEdited = null
                        }
                    }
                )
            }
        }
    }
}