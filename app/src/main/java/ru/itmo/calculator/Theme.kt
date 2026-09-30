package ru.itmo.calculator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object LeafColors {
    val Surface = Color(0xFFFCFCFC)
    val Key = Color(0xFFF3F3F3)
    val KeyPressed = Color(0xFFE3E3E3)
    val OnKey = Color(0xFF4A4A4A)
    val Preview = Color(0xFF858585)
    val Primary = Color(0xFF558B2F)
    val Equals = Color(0xFF689F38)
    val OnEquals = Color(0xFFFFFFFF)
    val Clear = Color(0xFFD32F2F)
    val Icon = Color(0xFF6E6E6E)
    val IconDisabled = Color(0xFFBDBDBD)
}

@Composable
fun LeafTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = LeafColors.Primary,
            background = LeafColors.Surface,
            surface = LeafColors.Surface,
            onSurface = LeafColors.OnKey,
        ),
        content = content,
    )
}
