package com.example.greatimagedownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.greatimagedownloader.ui.MainScreenStateMachine
import com.example.greatimagedownloader.ui.theme.GReatImageDownloaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GReatImageDownloaderTheme {
                // A surface container using the 'background' color from the theme
                Scaffold {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        MainScreenStateMachine()
                    }
                }
            }
        }
    }
}
