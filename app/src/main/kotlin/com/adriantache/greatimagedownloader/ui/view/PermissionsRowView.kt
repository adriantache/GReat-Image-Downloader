package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsRowView(
    modifier: Modifier = Modifier,
    text: String,
    isGranted: Boolean,
    icon: @Composable () -> Unit = { DefaultIcon(isGranted) },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(10f),
            text = text,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(
            Modifier
                .weight(1f)
                .requiredSizeIn(minWidth = 16.dp)
        )

        icon()
    }
}

@Composable
private fun DefaultIcon(isEnabled: Boolean) {
    val icon = if (isEnabled) {
        android.R.drawable.button_onoff_indicator_on
    } else {
        android.R.drawable.button_onoff_indicator_off
    }

    Image(
        painter = painterResource(id = icon),
        contentDescription = null,
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionsRowViewPreview() {
    Column {
        PermissionsRowView(
            text = "Permission etc. xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            isGranted = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionsRowView(
            text = "Permission etc. xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            isGranted = false,
        )
    }
}
