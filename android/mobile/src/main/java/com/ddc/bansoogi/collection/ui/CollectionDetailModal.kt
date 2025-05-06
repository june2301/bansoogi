package com.ddc.bansoogi.collection.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.collection.data.model.CollectionDto
import io.realm.kotlin.types.RealmInstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CollectionDetailDialog(character: CollectionDto, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val gifResId = context.resources.getIdentifier(character.gifUrl, "drawable", context.packageName)

    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(gifResId)
            .build(),
        imageLoader = imageLoader
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
        icon = {
            Image(
                painter = painter,
                contentDescription = character.title,
                modifier = Modifier
                    .size(160.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = character.title,
                    style = MaterialTheme.typography.titleLarge
                )
                character.acquisitionCount?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${it}회",
                        color = Color(0xFFEE7777),
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFBD752),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = character.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
                character.createdAt?.let {
                    Text(
                        text = "${formatDate(it)} 획득",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { /* 프로필 등록 로직 */ }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(" 프로필 사진 등록")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { /* 다운로드 로직 */ }) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text(" 사진 다운로드")
                }
            }
        }
    )
}

fun formatDate(instant: RealmInstant): String {
    val millis = instant.epochSeconds * 1000 + instant.nanosecondsOfSecond / 1_000_000
    val date = Date(millis)
    val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    return formatter.format(date)
}