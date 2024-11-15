package com.adriantache.greatimagedownloader.data.utils

import android.content.SharedPreferences

fun SharedPreferences.getString(key: String): String? {
    return this.getString(key, null)
}
