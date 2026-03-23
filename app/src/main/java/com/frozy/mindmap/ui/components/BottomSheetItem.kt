package com.frozy.mindmap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.ui.theme.MindMapShapes

@Composable
fun BottomSheetItem(
    icon: ImageVector,
    contentDescription: String? = null,
    text: String,
    itemOnClick: () -> Unit,
    isClickable: Boolean = true
){
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MindMapShapes.medium,
        modifier = Modifier
            .clickable(
                onClick = { itemOnClick() },
                enabled = isClickable
            )
            .fillMaxWidth()
    ) {
        Row {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
            )
            Text(
                text = text,
                modifier = Modifier.padding(all = 8.dp)
            )
        }
    }
}
