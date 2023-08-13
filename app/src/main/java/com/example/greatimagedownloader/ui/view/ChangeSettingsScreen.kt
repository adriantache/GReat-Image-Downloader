package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.domain.model.Settings
import com.example.greatimagedownloader.domain.model.States

@Composable
fun ChangeSettingsScreen(state: States.ChangeSettings) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CloseIcon(state.onExitSettings)
        }

        item {
            SettingsRow(
                primaryText = "Remember last downloaded photos",
                secondaryText = "Enable this to store what the latest photo in every folder is, enabling you to safely delete older files.",
                initialValue = state.settings.rememberLastDownloadedPhotos,
                onSelect = state.onRememberLastDownloadedPhotos,
            )
        }

        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = state.settings.rememberLastDownloadedPhotos == true,
                        onClick = { state.onDeleteAllPhotos() },
                    ),
                text = "Delete all downloaded media.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangeSettingsScreenPreview() {
    ChangeSettingsScreen(
        state = States.ChangeSettings(
            settings = Settings(),
            onRememberLastDownloadedPhotos = {},
            onDeleteAllPhotos = {},
            onExitSettings = {},
        )
    )
}