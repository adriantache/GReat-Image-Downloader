package com.adriantache.greatimagedownloader.ui.util

import androidx.compose.ui.Modifier

fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) this.then(modifier()) else this
}
