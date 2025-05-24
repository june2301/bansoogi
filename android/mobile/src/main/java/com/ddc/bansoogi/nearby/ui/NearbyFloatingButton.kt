package com.ddc.bansoogi.nearby.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun NearbyFloatingButton(
    isSearching: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isSearching) Color(0xFF4CAF50) else Color(0xFF2196F3)
    ) {
        Text(
            text = if (isSearching) "🔍" else "📡",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}