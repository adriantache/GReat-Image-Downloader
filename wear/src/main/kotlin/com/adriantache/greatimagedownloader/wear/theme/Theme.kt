package com.adriantache.greatimagedownloader.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearColors = Colors(
    primary = RicohYellow,
    primaryVariant = RicohYellowDark,
    secondary = DarkCard,
    secondaryVariant = DarkSurface,
    background = DarkBackground,
    surface = DarkSurface,
    error = ErrorRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White,
)

@Composable
fun WearTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = WearColors,
        content = content,
    )
}
