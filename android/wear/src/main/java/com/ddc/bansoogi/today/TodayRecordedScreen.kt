package com.ddc.bansoogi.today

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.R
import com.ddc.bansoogi.main.MenuButton
import com.ddc.bansoogi.main.MenuScreen

@Composable
fun TodayRecordedScreen(report: ReportDto) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_sunny_sky), // 원하는 배경
            contentDescription = "배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x60000000))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "오늘 기록",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            InfoRow("누워있던 시간", report.lyingTime, "분")
            VerticalSpacer()
            InfoRow("앉아있던 시간", report.sittingTime, "분")
            VerticalSpacer()
            InfoRow("휴대폰 사용 시간", report.phoneTime, "분")
            VerticalSpacer()
            InfoRow("기상 이벤트", report.standupCount, "회")
            VerticalSpacer()
            InfoRow("스트레칭 이벤트", report.stretchCount, "회")
            VerticalSpacer()
            InfoRow("휴대폰 미사용 이벤트", report.phoneOffCount, "회")
        }
    }
}

@Composable
fun VerticalSpacer() {
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun InfoRow(label: String, value: Int, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.White, shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Black, fontSize = 14.sp)
        Text(text = "$value$unit", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultTodayPreview() {
    TodayRecordedScreen(
        report = ReportDto(
            energyPoint = 65,

            standupCount = 3,
            stretchCount = 2,
            phoneOffCount = 1,

            lyingTime = 40,
            sittingTime = 120,
            phoneTime = 90,
            sleepTime = 360,

            walkCount = 1628,
            runTime = 3,
            exerciseTime = 0,
            stairsClimbed = 1,

            breakfast = true,
            lunch = true,
            dinner = false
        )
    )
}