package com.example.prototype.presentation.theme

import androidx.wear.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun protoTypeTheme(content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize for your app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    MaterialTheme(
        content = content
    )
}