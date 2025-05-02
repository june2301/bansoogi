package com.example.eggi.main.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.eggi.R
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.example.eggi.calendar.ui.RecordedModal
import com.example.eggi.common.data.model.TodayRecord
import com.example.eggi.main.controller.TodayRecordController
import com.example.eggi.main.view.TodayRecordView

@Preview
@Composable
fun HomeScreen() {

    var todayRecordState = remember { mutableStateOf<TodayRecord?>(null) }
    var view = remember {
        object : TodayRecordView {
            override fun displayTodayRecord(todayRecord: TodayRecord) {
                todayRecordState.value = todayRecord
            }

            override fun showEmptyState() {
                todayRecordState.value = null
            }
        }
    }
    val todayRecordController = remember { TodayRecordController(view) }
    LaunchedEffect(Unit) {
        todayRecordController.initialize()
    }

    todayRecordState.value?.let { todayRecord ->
        // 메인 페이지
        if (true) {
            HomeContent(todayRecord, todayRecordController)
        }
        // 알 받기
        else {
            // TODO: BeforeContent(todayRecord)
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("로딩 중...", fontSize = 16.sp)
    }
}

@Composable
fun HomeContent(todayRecord: TodayRecord, todayRecordController: TodayRecordController) {
    var progressValue by remember { mutableStateOf(todayRecord.energyPoint) }
    var showModal by remember { mutableStateOf(false) }

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
                    .fillMaxWidth(progressValue / 100f)
                    .height(24.dp)
                    .background(Color.Green)
            )
            Text(
                text = "$progressValue / 100",
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
        Button(
            onClick = {
                if (progressValue < 100) {
                    progressValue += 5
                    todayRecordController.updateInteractionCnt(todayRecord.recordId)
                    todayRecordController.updateEnergy(todayRecord.recordId, 5)
                }
            },
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
        if (!todayRecord.isClosed) {
            DayTimeModal(
                todayRecord = todayRecord,
                onDismissRequest = { showModal = false },
                onNavigateToToday = {
                    // TODO: 콜백 호출 -> (데이터) 필요한 작업 수행
                    showModal = false
                }
            )
        }
        else {
            // TODO: 결산 모달
            RecordedModal(
                onDismissRequest = { showModal = false },
                selectedDate = "2025-04-20"
            )
        }
    }
}
