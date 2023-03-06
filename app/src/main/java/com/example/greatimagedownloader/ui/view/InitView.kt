package com.example.greatimagedownloader.ui.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun InitView(
    onReady: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onReady()
    }

    Text("Init")
}
