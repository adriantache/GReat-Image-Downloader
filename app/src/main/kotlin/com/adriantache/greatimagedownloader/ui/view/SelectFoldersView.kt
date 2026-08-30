package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adriantache.greatimagedownloader.domain.model.FolderInfo

@Composable
fun SelectFoldersView(
    folderInfo: FolderInfo,
    onFoldersSelect: (List<String>) -> Unit,
) {
    var selectedFolders by remember { mutableStateOf(folderInfo.folders.keys) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Multiple image folders found. Please select desired folders to download from:")

        LazyColumn {
            items(folderInfo.folders.toList()) {
                val (name, files) = it
                val onClick = { _: Boolean ->
                    selectedFolders = if (selectedFolders.contains(name)) {
                        selectedFolders - name
                    } else {
                        selectedFolders + name
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = true, onClick = { onClick(true) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = selectedFolders.contains(name), onCheckedChange = onClick)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("$name ($files files)")
                }
            }
        }

        Button(onClick = { onFoldersSelect(selectedFolders.toList()) }) {
            Text("Confirm")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectFoldersViewPreview() {
    val folderInfo = FolderInfo(
        mapOf(
            "first" to 123,
            "second" to 17,
        )
    )

    SelectFoldersView(folderInfo) {}
}
