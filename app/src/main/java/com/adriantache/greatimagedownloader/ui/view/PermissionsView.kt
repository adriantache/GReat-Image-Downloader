package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsView(
    isLocationPermissionGranted: Boolean,
    isPhotosPermissionGranted: Boolean,
    isNotificationsPermissionGranted: Boolean,
    onRequestPermissions: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Text(
            text = "In order for this app to work, we need the following access:",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(Modifier.height(16.dp))

        PermissionsRowView(
            text = "Location access - for scanning of and connecting to WiFi",
            isGranted = isLocationPermissionGranted,
        )

        Spacer(Modifier.height(8.dp))

        PermissionsRowView(
            text = "Photos access - for checking existing and downloading new photos",
            isGranted = isPhotosPermissionGranted,
        )

        Spacer(Modifier.height(8.dp))

        PermissionsRowView(
            text = "Show notifications - for the service that downloads the files",
            isGranted = isNotificationsPermissionGranted,
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = onRequestPermissions) {
            Text("Grant permissions")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionsViewPreview() {
    PermissionsView(
        isLocationPermissionGranted = true,
        isPhotosPermissionGranted = false,
        isNotificationsPermissionGranted = false,
    ) {}
}
