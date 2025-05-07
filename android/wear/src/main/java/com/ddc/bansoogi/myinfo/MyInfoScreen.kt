package com.ddc.bansoogi.myinfo

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.ddc.bansoogi.today.ReportDto

@Composable
fun MyInfoScreen(myInfo: MyInfoDto) {
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
                text = "내 정보",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            InfoRow("기상 희망 시간", unit = myInfo.wakeUpTime)
            VerticalSpacer()
            InfoRow("취침 희망 시간", unit = myInfo.wakeUpTime)
            VerticalSpacer()
            InfoRow("아침 희망 시간", unit = myInfo.wakeUpTime)
            VerticalSpacer()
            InfoRow("점심 희망 시간", unit = myInfo.wakeUpTime)
            VerticalSpacer()
            InfoRow("저녁 희망 시간", unit = myInfo.wakeUpTime)
            VerticalSpacer()
            ToggleRow("알림 설정", myInfo.alarmEnabled)
            VerticalSpacer()
            ToggleRow("배경음 설정", myInfo.bgSoundEnabled)
            VerticalSpacer()
            ToggleRow("효과음 설정", myInfo.effectSoundEnabled)
        }
    }
}

@Composable
fun VerticalSpacer() {
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun InfoRow(label: String, value: Int? = null, unit: String = "") {
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
        Text(text = unit, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.White, shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Black, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = {},
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF99CC00)
            )
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultMyInfoPreview() {
    MyInfoScreen(
        myInfo = MyInfoDto(
            wakeUpTime = "07:30",
            sleepTime = "23:00",
            breakfastTime = "08:00",
            lunchTime = "12:00",
            dinnerTime = "18:30",
            notificationDuration = 30,
            alarmEnabled = true,
            bgSoundEnabled = false,
            effectSoundEnabled = true
        )
    )
}