package com.frozy.mindmap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
//allows use of res/ stuff
import com.frozy.mindmap.R

//fontFamilies for body text and display (e.g title) text, gets the fonts from res/font
val bodyFontFamily = FontFamily(Font(resId = R.font.acme_regular))
val displayFontFamily = FontFamily(Font(resId = R.font.story_script_regular))


// Default Material 3 typography values
val baseline = Typography()

//Copies the default attributes from Typography() but changes the fontFamily
val MindMapTypography = Typography(
    displayLarge =   baseline.displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium =  baseline.displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall =   baseline.displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge =  baseline.headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall =  baseline.headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge =     baseline.titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium =    baseline.titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall =     baseline.titleSmall.copy(fontFamily = displayFontFamily),

    bodyLarge =      baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium =     baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall =      baseline.bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge =     baseline.labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium =    baseline.labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall =     baseline.labelSmall.copy(fontFamily = bodyFontFamily),
)

