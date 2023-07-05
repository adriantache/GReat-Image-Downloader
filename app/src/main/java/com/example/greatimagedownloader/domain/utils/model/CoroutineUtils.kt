package com.example.greatimagedownloader.domain.utils.model

import kotlinx.coroutines.delay

suspend fun delay(timeMillis: Int) {
    delay(timeMillis.toLong())
}
