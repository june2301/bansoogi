package com.ddc.bansoogi.collection.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.collection.data.model.CollectionDto
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.myInfo.data.entity.User
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.launch
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("RememberReturnType")
@Composable
fun CollectionDetailDialog(
    character: CollectionDto,
    fullList: List<CollectionDto>,
    onDismiss: () -> Unit) {
    val context = LocalContext.current
    val model = remember { MyInfoModel() }
    val scope = rememberCoroutineScope()

    val gifResId = context.resources.getIdentifier(character.gifUrl, "drawable", context.packageName)
    var showConfirmDialog: Boolean by remember { mutableStateOf(false) }

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
            TextButton(onClick = onDismiss) {
                Text(
                    "닫기",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
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
                    Spacer(modifier = Modifier.height(8.dp))
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

                OutlinedButton(
                    onClick = {
                        showConfirmDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF888888)
                    )
                    Text(
                        " 프로필 사진 등록",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888)
                    )
                }

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        model.updateProfileBansoogiId(character.id)
                                        Toast.makeText(context, "프로필이 변경되었습니다", Toast.LENGTH_SHORT).show()
                                        showConfirmDialog = false
                                        onDismiss()
                                    }
                                }
                            ) {
                                Text(
                                    "그럼!",
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text(
                                    "아니.",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        title = {
                            Text(
                                "프로필 사진으로 등록할까요?",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ) },
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { /* 다운로드 로직 */ },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF888888)
                    )
                    Text(
                        " 사진 다운로드",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888)
                    )
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