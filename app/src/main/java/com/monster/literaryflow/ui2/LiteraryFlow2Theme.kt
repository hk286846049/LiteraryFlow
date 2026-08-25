package com.monster.literaryflow.ui2

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LfBackground = Color(0xFFF1F9F6)
val LfPrimary = Color(0xFF00A885)
val LfPrimarySoft = Color(0xFFD1F2E6)
val LfSkySoft = Color(0xFFDEEFF7)
val LfWarningSoft = Color(0xFFFFF2C7)
val LfDanger = Color(0xFFD13D47)
val LfDangerSoft = Color(0xFFFFE5E5)
val LfInk = Color(0xFF172333)
val LfMuted = Color(0xFF617080)
val LfDark = Color(0xFF0E1B2A)
val LfCard = Color.White

private val LiteraryFlow2Colors = lightColorScheme(
    primary = LfPrimary,
    onPrimary = Color.White,
    background = LfBackground,
    onBackground = LfInk,
    surface = LfCard,
    onSurface = LfInk,
    secondary = LfSkySoft,
    error = LfDanger
)

@Composable
fun LiteraryFlow2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiteraryFlow2Colors,
        content = content
    )
}
