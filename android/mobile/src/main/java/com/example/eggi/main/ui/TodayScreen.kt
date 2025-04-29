package com.example.eggi.main.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.eggi.R

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
            painter = painterResource(id = R.drawable.egg_before_broken),
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
    value: Int,
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
                text = value.toString(),
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
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
fun TodayScreen(
    onDismissRequest: () -> Unit,
    onNavigateToToday: () -> Unit
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
                            value = 2,
                            unit = "분"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "앉아있던 시간 :",
                            value = 43,
                            unit = "분"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "휴대폰 사용 시간 :",
                            value = 38,
                            unit = "분"
                        )

                        Divider()

                        // 이벤트 행동 변화 섹션
                        SectionHeader(
                            title = "이벤트 행동 변화"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "기상 이벤트 :",
                            value = 0,
                            unit = "회"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "스트레칭 이벤트 :",
                            value = 1,
                            unit = "회"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "휴대폰 미사용 이벤트 :",
                            value = 0,
                            unit = "회"
                        )

                        Divider()

                        // 건강 정보 섹션
                        SectionHeader(
                            title = "건강 정보"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "평균 심박수 :",
                            value = 71,
                            unit = "회"
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(0.1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onNavigateToToday,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E616A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("확인", color = Color.White)
                    }
                }
            }
        }
    }
}
