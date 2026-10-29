package com.adriantache.greatimagedownloader.ui.permissions

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsRequester(
    onPermissionsGranted: () -> Unit,
    content: @Composable (
        isLocationPermissionGranted: Boolean,
        isPhotosPermissionGranted: Boolean,
        isNotificationsPermissionGranted: Boolean,
        isLocalNetworkPermissionGranted: Boolean,
        isBluetoothPermissionGranted: Boolean,
        onRequestPermissions: () -> Unit,
    ) -> Unit,
) {
    val photosPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        READ_MEDIA_IMAGES
    } else {
        READ_EXTERNAL_STORAGE
    }

    val permissionsList = remember {
        buildList {
            add(ACCESS_FINE_LOCATION)
            add(photosPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(BLUETOOTH_CONNECT)
                add(BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= 37) {
                add("android.permission.ACCESS_LOCAL_NETWORK")
            }
        }
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissions = permissionsList)

    val isLocationPermissionGranted = multiplePermissionsState.permissions
        .firstOrNull { it.permission == ACCESS_FINE_LOCATION }?.status?.isGranted ?: false

    val isPhotosPermissionGranted = multiplePermissionsState.permissions
        .firstOrNull { it.permission == photosPermission }?.status?.isGranted ?: false

    val isNotificationsPermissionGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        multiplePermissionsState.permissions
            .firstOrNull { it.permission == POST_NOTIFICATIONS }?.status?.isGranted ?: false
    }

    val isBluetoothPermissionGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        true
    } else {
        val connectGranted = multiplePermissionsState.permissions
            .firstOrNull { it.permission == BLUETOOTH_CONNECT }?.status?.isGranted == true
        val scanGranted = multiplePermissionsState.permissions
            .firstOrNull { it.permission == BLUETOOTH_SCAN }?.status?.isGranted == true
        connectGranted && scanGranted
    }

    val isLocalNetworkPermissionGranted = if (Build.VERSION.SDK_INT < 37) {
        true
    } else {
        multiplePermissionsState.permissions
            .firstOrNull { it.permission == "android.permission.ACCESS_LOCAL_NETWORK" }?.status?.isGranted ?: true
    }

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    content(
        /* isLocationPermissionGranted */ isLocationPermissionGranted,
        /* isPhotosPermissionGranted */ isPhotosPermissionGranted,
        /* isNotificationsPermissionGranted */ isNotificationsPermissionGranted,
        /* isLocalNetworkPermissionGranted */ isLocalNetworkPermissionGranted,
        /* isBluetoothPermissionGranted */ isBluetoothPermissionGranted,
    ) /* onRequestPermissions */ {
        if (multiplePermissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        } else {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }
}
