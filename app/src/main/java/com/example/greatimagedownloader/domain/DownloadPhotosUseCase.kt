package com.example.greatimagedownloader.domain

import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.States
import com.example.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.flow.MutableStateFlow

interface DownloadPhotosUseCase {
    val state: MutableStateFlow<States>
    val event: MutableStateFlow<Event<Events>?>
}
