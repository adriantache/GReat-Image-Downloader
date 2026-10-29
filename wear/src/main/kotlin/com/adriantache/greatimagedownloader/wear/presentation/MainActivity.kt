package com.adriantache.greatimagedownloader.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.adriantache.greatimagedownloader.wear.data.WearCommunicationClient
import com.adriantache.greatimagedownloader.wear.theme.WearTheme

class MainActivity : ComponentActivity() {
    private lateinit var communicationClient: WearCommunicationClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        communicationClient = WearCommunicationClient(applicationContext)

        setContent {
            WearTheme {
                MainWearScreen(client = communicationClient)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        communicationClient.startListening()
    }

    override fun onPause() {
        super.onPause()
        communicationClient.stopListening()
    }
}
