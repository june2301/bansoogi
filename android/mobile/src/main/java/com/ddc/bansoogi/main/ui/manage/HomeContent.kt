package com.ddc.bansoogi.main.ui.manage

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.ui.RecordedModal
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.notification.NotificationDispatcher
import com.ddc.bansoogi.common.notification.NotificationFactory
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.DayTimeModal
import com.ddc.bansoogi.main.ui.handle.handleInteraction
import com.ddc.bansoogi.main.ui.util.getRemainingCooldownMillis
import com.ddc.bansoogi.main.ui.util.isInteractionConditionMet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeContent(
    todayRecordDto: TodayRecordDto,
    todayRecordController: TodayRecordController,
    isInSleepRange: Boolean,
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showModal by remember { mutableStateOf(false) }

    // TODO: 알림 테스트용 - 나중에 삭제
    var notified20 by remember { mutableStateOf(todayRecordDto.energyPoint >= 20) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(horizontal = 16.dp)
                .background(Color.White)
                .border(
                    width = 2.dp,
                    color = Color.DarkGray
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(0xFFEEEEEE))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(todayRecordDto.energyPoint / 100f)
                    .height(24.dp)
                    .background(Color.Green)
            )
            Text(
                text = "${todayRecordDto.energyPoint} / 100",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 50.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            add(GifDecoder.Factory())
                            add(ImageDecoderDecoder.Factory())
                        }
                        .build()
                }

                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(R.drawable.ic_today)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = "버튼 이미지",
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clickable {
                            showModal = true
                            onModalOpen()
                        },
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Today",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val imageLoader = remember {
                ImageLoader.Builder(context)
                    .components {
                        add(GifDecoder.Factory())
                        add(ImageDecoderDecoder.Factory())
                    }
                    .build()
            }

            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(R.drawable.bansoogi_basic)
                        .build(),
                    imageLoader = imageLoader
                ),
                contentDescription = "반숙이",
                modifier = Modifier
                    .width(420.dp)
                    .height(540.dp),
                contentScale = ContentScale.Fit
            )
        }

        val buttonShape = RoundedCornerShape(30.dp)
        val isCoolDown = remember { mutableStateOf(true) }

        LaunchedEffect(todayRecordDto.interactionLatestTime) {
            val remainingTime = getRemainingCooldownMillis(todayRecordDto.interactionLatestTime)
            if (remainingTime > 0) {
                isCoolDown.value = true
                delay(remainingTime)
            }
            isCoolDown.value = false
        }

        Button(
            onClick = {
                // TODO: 상호작용 애니메이션 출력

                todayRecordController.onInteract(todayRecordDto, isInSleepRange)

                if (!isInSleepRange && todayRecordDto.energyPoint < 100) { // -> 함수 내부에 범위 체크
                    // TODO: 알림 테스트용 - 나중에 삭제
                    if (todayRecordDto.energyPoint >= 20 && !notified20) {
                        notified20 = true
                        NotificationDispatcher.show(
                            context,
                            NotificationDispatcher.Id.PHONE,
                            NotificationFactory.phoneUsage(context, 20)
                        )
                    }

                }
            },
            enabled = !isInSleepRange && !isCoolDown.value && isInteractionConditionMet(todayRecordDto),
            modifier = Modifier
                .padding(vertical = 10.dp)
                .height(60.dp)
                .fillMaxWidth(0.4f)
                .border(
                    width = 4.dp,
                    color = Color.DarkGray,
                    shape = buttonShape
                ),
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF2E616A)
            )
        ) {
            Text(
                text = "상호작용",
                color = Color(0xFF2E616A),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showModal) {
        if (!isInSleepRange) {
            DayTimeModal(
                todayRecordDto = todayRecordDto,
                onDismissRequest = {
                    showModal = false
                    onModalClose() },
                onNavigateToToday = {
                    // TODO: 콜백 호출 -> (데이터) 필요한 작업 수행
                    showModal = false
                    onModalClose()
                },
                healthData = healthData,
            )
        }
        else {
            val formatDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            Log.d("date", formatDate)
            RecordedModal(
                onDismissRequest = { showModal = false },
                selectedDate = formatDate
            )
        }
    }
}
