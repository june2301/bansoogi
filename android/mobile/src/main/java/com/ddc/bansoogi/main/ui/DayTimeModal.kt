package com.ddc.bansoogi.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.util.health.CustomHealthData

@Composable
fun ModalHeader(
    title: String,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color(0xFF2E616A)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_today),
            contentDescription = "아이콘",
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.DarkGray
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.egg_before_broken_gif),
            contentDescription = "달걀",
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = fontSize,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: Int?,
    unit: String,
    modifier: Modifier = Modifier,
    highlightText: String? = null,
    highlightColor: Color = Color(0xFF4CAF50)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (highlightText != null) {
                Text(
                    text = highlightText,
                    fontSize = 14.sp,
                    color = highlightColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = value?.toString() ?: "",
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value?.let { "${unit.toString()} " }?: "해당 값이 존재하지 않습니다.",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun Divider() {
    androidx.compose.material3.Divider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = Color.LightGray,
        thickness = 1.dp
    )
}


@Composable
fun DayTimeModal(
    todayRecordDto: TodayRecordDto,
    onDismissRequest: () -> Unit,
    onNavigateToToday: () -> Unit,
    healthData: CustomHealthData,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {

                Box(modifier = Modifier.weight(0.1f)) {
                    ModalHeader(
                        title = "오늘 기록"
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()), // 넘쳤을 때, 스크롤
                        // 일정한 간격 설정
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 행동 기록 섹션
                        SectionHeader(
                            title = "행동 기록"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "누워있던 시간 :",
                            value = todayRecordDto.lyingTime,
                            unit = " 분"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "앉아있던 시간 :",
                            value = todayRecordDto.sittingTime,
                            unit = " 분"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "휴대폰 사용 시간 :",
                            value = todayRecordDto.phoneTime,
                            unit = " 분"
                        )

                        Divider()

                        // 이벤트 행동 변화 섹션
                        SectionHeader(
                            title = "이벤트 행동 변화"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "기상 이벤트 :",
                            value = todayRecordDto.standUpCnt,
                            unit = " 회"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "스트레칭 이벤트 :",
                            value = 1,
                            unit = " 회"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "휴대폰 미사용 이벤트 :",
                            value = 0,
                            unit = " 회"
                        )

                        Divider()

                        // 건강 정보 섹션
                        SectionHeader(
                            title = "건강 정보"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "총 걸음수 :",
                            value = healthData.step.toInt(),
                            unit = " 회"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "총 계단 수 :",
                            value = healthData.floorsClimbed.toInt(),
                            unit = " 계단"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "수면 시간 :",
                            value = healthData.sleepData,
                            unit = " 분"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "운동 시간 :",
                            value = healthData.exerciseTime,
                            unit = " 분"
                        )
                    }
                }
            }
        }
    }
}
