package com.example.greatimagedownloader.ui.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PermissionsView(
    onPermissionsGranted: () -> Unit,
) {
    Text("Request permissions")
}
