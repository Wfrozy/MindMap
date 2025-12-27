package com.frozy.mindmap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.frozy.mindmap.ui.theme.MindMapTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.frozy.mindmap.ui.theme.MindMapShapes
import com.frozy.mindmap.ui.theme.MindMapTypography
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.json.JSONObject

//todo put the file list in a DataStore
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

//todo change some of this
data class FileData(
    val fileName: String = "",
    val fileContent: String = "",
    val storage: StorageOption = StorageOption.DEVICE,
    val timeStampID: Long = System.currentTimeMillis()
)

//enum class to make code more readable
enum class StorageOption(val label: Int, val description: Int) {
    DEVICE(label = R.string.create_new_file_device_storage_label, description = R.string.create_new_file_device_storage_description),
    APP(label = R.string.create_new_file_app_storage_label,  description = R.string.create_new_file_app_storage_description)
}

fun checkIfFileNameIsInvalid(string: String): Boolean{
    return string.any{ it in "/\\:*?\"<>|"} ||
           string.isBlank()
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
    var isCreateDialogVisible by remember { mutableStateOf(value = false) }

    //this variable exists because i can't use context.getString() anymore inside sanitizeAndEnsureJsonExtension()
    val fallbackString = stringResource(id = R.string.default_map_name_with_json)

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

    //this is not in the ViewModel because it would be a pain to implement it there
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
                mavm.changeFileList(newValue = fileList + currentFileData.copy(
                    fileName= sanitizedFileNameNoJson,
                    storage = selectedStorage,
                    timeStampID = System.currentTimeMillis()
                ))
                Toast.makeText(context, toastFileCreatedSuccess, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, toastFileCreatedFail, Toast.LENGTH_LONG).show()
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
                    //debug button
                    IconButton(
                        //todo onclick
                        onClick = { debugButton() },
                        content = {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                tint = Color(0xFF23D715),
                                contentDescription = ""
                            )
                        }
                    )
                    IconButton(
                        onClick = { loadFileButton() },
                        content = {
                            Icon(
                                imageVector = Icons.Default.ImportExport,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = stringResource(R.string.contentDescription_import_file_iconButton)
                            )
                        }
                    )
                    IconButton(
                        //todo onclick
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
                onClick = { isCreateDialogVisible = true }
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    items(items = fileList, key = { it.timeStampID }) { file ->
                        val borderColor = when{
                            file.storage == StorageOption.APP -> MaterialTheme.colorScheme.inverseOnSurface
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
                        ){
                            val mapIconHeight = 56.dp
                            Column {
                                Icon(
                                    painter = painterResource(id = R.drawable.vecteezy_map),
                                    contentDescription = null,
                                    modifier = Modifier.size(width = 56.dp, height = mapIconHeight)
                                )
                            }
                            Column {
                                Text(
                                    text = file.fileName.removeSuffix(suffix = ".json"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 0.dp, end = 8.dp)
                                )
                                Text(
                                    text = "Map | ${stringResource(id = file.storage.label)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
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
                                        //todo onclick
                                        .clickable(onClick = { editMapSettings() })
                                        .size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(height = 8.dp))
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
                //if StorageOption.APP
                else {
                    coroutineScope.launch {
                        val uniqueFileName = FileIO.makeUniqueFilename(dir = context.filesDir, baseName = sanitizedFileName)
                        val jsonObj = JSONObject().applyBasicContent(
                            fileName = sanitizedFileName,
                            selectedStorage = selectedStorage,
                            fileContent = currentFileData
                        )
                        val jsonText = jsonObj.toString(2)

                        val isWriteSuccessful = FileIO.writeTextToFileInAppStorage(context, uniqueFileName, jsonText)
                        if (isWriteSuccessful) {
                            // Add to file list using the stored filename and storage
                            val newFileData = currentFileData.copy(
                                storage = StorageOption.APP,
                                fileName = uniqueFileName,
                                timeStampID = System.currentTimeMillis()
                            )
                            mavm.changeFileList(newValue = fileList + newFileData)
                            Toast.makeText(context, "Saved ${uniqueFileName.removeSuffix(suffix = ".json")}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save $sanitizedFileNameNoJson", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                isCreateDialogVisible = false
            },
            currentSelectedStorage = selectedStorage,
            onStorageOptionChange = { option -> selectedStorage = option }
        )
    }
}

//used in the floating action button
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

fun Context.openSettingsActivity(){
    val intent = Intent(this, SettingsActivity::class.java)
    /*
        if the activity already exists in the current task’s back stack, Android will
        destroy every Activity above it,
        bring the existing instance to the foreground,
        reuse it instead of creating a new one,
        and prevents recreation if the activity is already on top
    */
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    this.startActivity(intent)
}
fun loadFileButton(){}
fun debugButton(){}
fun editMapSettings(){}

fun openSelectedMap(context: Context, file: FileData){
    val intent = Intent(context, MapEditorActivity::class.java).apply {
        putExtra("file_name", file.fileName)
        putExtra("storage", file.storage.name)
        //todo file path???
    }
    /*
        if the activity already exists in the current task’s back stack, Android will
        destroy every Activity above it,
        bring the existing instance to the foreground,
        reuse it instead of creating a new one,
        and prevents recreation if the activity is already on top
    */
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    context.startActivity(intent)
}