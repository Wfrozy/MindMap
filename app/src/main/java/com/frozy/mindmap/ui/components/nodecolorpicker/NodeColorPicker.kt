package com.frozy.mindmap.ui.components.nodecolorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.ui.utils.lighten
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

//todo the border around the swatches sometimes gets stuck
//todo using a null background color when the border is already null makes it switch for some reason
//todo lazy list for the color swatches?
//
@Composable
fun NodeColorPicker(
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit,
    label: String,
    predefinedColors: List<Color>
) {
    var isCustomPickerVisible by remember { mutableStateOf(value = false) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── No color option ───────────────────────────────────────────
            NoColorSwatch(
                isSelected = selectedColor == null,
                onClick = { onColorSelected(null) }
            )

            // ── Predefined swatches ───────────────────────────────────────
            predefinedColors.forEach { color ->
                ColorSwatch(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorSelected(color) }
                )
            }

            // ── Custom color button ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(size = 36.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Red, Color.Yellow,
                                Color.Green, Color.Cyan,
                                Color.Blue, Color.Magenta,
                                Color.Red
                            )
                        )
                    )
                    .border(
                        width = if (selectedColor != null && selectedColor !in predefinedColors) 2.dp else 0.dp,
                        color = if (selectedColor == MaterialTheme.colorScheme.tertiary) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        shape = CircleShape
                    )
                    .clickable { isCustomPickerVisible = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Colorize,
                    contentDescription = stringResource(id = R.string.contentDescription_custom_color_selector),
                    tint = Color.Black,
                    modifier = Modifier.size(size = 18.dp)
                )
                Icon(
                    imageVector = Icons.Default.Colorize,
                    contentDescription = stringResource(id = R.string.contentDescription_custom_color_selector),
                    tint = Color.White,
                    modifier = Modifier.size(size = 16.dp)
                )
            }
        }
    }

    if (isCustomPickerVisible) {
        CustomColorPickerDialog(
            initialColor = selectedColor ?: Color.White,
            onColorConfirmed = { color ->
                onColorSelected(color)
                isCustomPickerVisible = false
            },
            onDismiss = { isCustomPickerVisible = false }
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .size(size = 36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                other = if (isSelected) Modifier.border(width = 2.dp, borderColor, CircleShape)
                else Modifier
            )
            .clickable { onClick() }
    )
}

@Composable
private fun NoColorSwatch(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.tertiary
    val strokeColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(size = 36.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) borderColor else strokeColor.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        //diagonal strikethrough line
        Canvas(modifier = Modifier.size(size = 36.dp)) {
            drawLine(
                color = strokeColor.copy(alpha = 0.5f),
                start = Offset(x = size.width * 0.2f, y = size.height * 0.8f),
                end   = Offset(x = size.width * 0.8f, y = size.height * 0.2f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Color,
    onColorConfirmed: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val controller = rememberColorPickerController()

    // track the current color from the picker
    val currentColor by controller.selectedColor

    // rgb text field states
    var rText by remember { mutableStateOf(value = (initialColor.red * 255).toInt().toString()) }
    var gText by remember { mutableStateOf(value = (initialColor.green * 255).toInt().toString()) }
    var bText by remember { mutableStateOf(value = (initialColor.blue * 255).toInt().toString()) }

    //keep text fields in sync when the picker moves but only if the user isn't currently typing in a field
    var isTyping by remember { mutableStateOf(value = false) }
    LaunchedEffect(key1 = currentColor) {
        if (!isTyping) {
            rText = (currentColor.red * 255).toInt().toString()
            gText = (currentColor.green * 255).toInt().toString()
            bText = (currentColor.blue * 255).toInt().toString()
        }
    }

    fun onRgbTextChanged(r: String, g: String, b: String) {
        isTyping = true
        rText = r; gText = g; bText = b

        val rVal = r.toIntOrNull()?.coerceIn(0, 255)
        val gVal = g.toIntOrNull()?.coerceIn(0, 255)
        val bVal = b.toIntOrNull()?.coerceIn(0, 255)

        if (rVal != null && gVal != null && bVal != null) {
            controller.selectByColor(
                color = Color(red = rVal, green = gVal, blue = bVal),
                fromUser = true
            )
        }
        isTyping = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Custom color") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 250.dp),
                    controller = controller,
                    initialColor = initialColor
                )
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 24.dp),
                    controller = controller,
                )

                // ── RGB inputs ────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // color preview swatch
                    Box(
                        modifier = Modifier
                            .size(size = 40.dp)
                            .clip(shape = RoundedCornerShape(size = 8.dp))
                            .background(color = currentColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(size = 8.dp)
                            )
                    )

                    RgbTextField(
                        label = "R",
                        value = rText,
                        modifier = Modifier.weight(weight = 1f),
                        onValueChange = { onRgbTextChanged(r = it, g = gText, b = bText) }
                    )
                    RgbTextField(
                        label = "G",
                        value = gText,
                        modifier = Modifier.weight(weight = 1f),
                        onValueChange = { onRgbTextChanged(r = rText, g = it, b = bText) },
                    )
                    RgbTextField(
                        label = "B",
                        value = bText,
                        modifier = Modifier.weight(weight = 1f),
                        onValueChange = { onRgbTextChanged(r = rText, g = gText, b = it) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorConfirmed(currentColor) }) {
                Text(text = "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun RgbTextField(
    label: String,
    value: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                //only allow up to 3 chars and only allow digits
                if (input.length <= 3 && input.all { it.isDigit() }) {
                    onValueChange(input)
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}