package com.adriantache.greatimagedownloader.domain.utils.model

class Event<T>(private val data: T) {
    private var isConsumed = false

    val value: T?
        get() {
            if (isConsumed) return null

            isConsumed = true
            return data
        }
}
