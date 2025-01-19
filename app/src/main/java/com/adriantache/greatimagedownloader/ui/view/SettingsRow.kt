package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    primaryText: String,
    secondaryText: String? = null,
    initialValue: Boolean? = null,
    onSelect: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .requiredHeightIn(min = 48.dp)) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )

            if (secondaryText != null) {
                Spacer(Modifier.height(4.dp))

                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Checkbox(checked = initialValue ?: false, onCheckedChange = { onSelect() })
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowPreview() {
    SettingsRow(
        modifier = Modifier.padding(16.dp),
        primaryText = "Setting",
        onSelect = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowPreview2() {
    SettingsRow(
        modifier = Modifier.padding(16.dp),
        primaryText = "Setting",
        secondaryText = "This setting sets settings",
        initialValue = true,
        onSelect = {},
    )
}
