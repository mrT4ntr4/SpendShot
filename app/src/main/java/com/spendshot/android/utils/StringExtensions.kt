package com.spendshot.android.utils

import java.util.Locale

fun String.toTitleCase(): String {
    if (this.isEmpty()) return this
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
    }
}
