package com.example.greatimagedownloader.data.utils

fun <T : Number> List<T>.mode(): T {
    return this.groupBy { it }.maxBy { it.value.size }.key
}
