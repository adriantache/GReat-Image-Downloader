package com.example.greatimagedownloader.domain

import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.States
import com.example.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.flow.StateFlow

interface DownloadPhotosUseCase {
    val state: StateFlow<States>
    val event: StateFlow<Event<Events>?>
}
