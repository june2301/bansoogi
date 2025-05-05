package com.example.eggi.collection.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eggi.collection.data.model.CollectionDto

@Composable
fun CollectionDetailDialog(character: CollectionDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
        title = { Text(text = character.title) },
        text = { Text(text = character.description) },
        icon = {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.title,
                modifier = Modifier.size(96.dp)
            )
        }
    )
}