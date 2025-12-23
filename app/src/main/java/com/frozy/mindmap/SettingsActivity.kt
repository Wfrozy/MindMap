package com.frozy.mindmap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.frozy.mindmap.MapEditorUI
import com.frozy.mindmap.StorageOption
import com.frozy.mindmap.ui.theme.MindMapTheme

class SettingsActivity : ComponentActivity() {
    //    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        setIntent(intent)
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MindMapTheme {
                SettingsActivityUI()
            }
        }
    }
}

@Composable
fun SettingsActivityUI(){

}
