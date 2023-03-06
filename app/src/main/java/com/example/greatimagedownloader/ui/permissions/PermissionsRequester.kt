package com.example.greatimagedownloader.ui.permissions

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsRequester(
    onPermissionsGranted: () -> Unit,
    content: @Composable () -> Unit,
) {
    val photosPermissionState = rememberMultiplePermissionsState(
        permissions = getPermissions()
    ) {
        // Handled in LaunchedEffect
    }

    LaunchedEffect(photosPermissionState) {
        if (!photosPermissionState.allPermissionsGranted) {
            photosPermissionState.launchMultiplePermissionRequest()
        } else {
            onPermissionsGranted()
        }
    }

    if (!photosPermissionState.allPermissionsGranted) {
        content()
    }
}

private fun getPermissions(): List<String> {
    val readPhotosPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    return listOf(readPhotosPermission)
}
