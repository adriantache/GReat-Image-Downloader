package com.adriantache.greatimagedownloader.domain

import com.adriantache.greatimagedownloader.domain.model.Events
import com.adriantache.greatimagedownloader.domain.model.States
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.flow.StateFlow

interface DownloadPhotosUseCase {
    val state: StateFlow<States>
    val event: StateFlow<Event<Events>?>
}
