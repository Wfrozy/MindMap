package com.frozy.mindmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.frozy.mindmap.ui.theme.MindMapTheme

class SettingsActivity : ComponentActivity() {
    //todo DataStore stuff
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MindMapTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = { finish() },
                                    content = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(id = R.string.contentDescription_back_button)
                                        )
                                    }
                                )
                            },
                            title = {
                                Text(
                                    text = stringResource(id = R.string.settings_title)
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ){
                        Column {
                            nothing()
                        }
                    }
                }
            }
        }
    }
}
fun nothing(){}
