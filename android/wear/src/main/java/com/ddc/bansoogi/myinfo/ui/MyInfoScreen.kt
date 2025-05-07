package com.ddc.bansoogi.myinfo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.common.ui.BackgroundImage
import com.ddc.bansoogi.common.ui.InfoRow
import com.ddc.bansoogi.common.ui.InfoSection
import com.ddc.bansoogi.common.ui.OverlayBackground
import com.ddc.bansoogi.common.ui.ScreenHeader
import com.ddc.bansoogi.common.ui.ToggleRow
import com.ddc.bansoogi.common.ui.VerticalSpacer
import com.ddc.bansoogi.common.util.calculateBoxHeight
import com.ddc.bansoogi.myinfo.data.MyInfoDto

@Composable
fun MyInfoScreen() {
    val myInfo = MyInfoDto(
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

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BackgroundImage()

        OverlayBackground()

        MyInfoContent(myInfo)
    }
}

@Composable
fun MyInfoContent(myInfo: MyInfoDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 8.dp
            )
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("내 정보")

        InfoSection(
            items = listOf(
                { InfoRow("기상 희망 시간", myInfo.wakeUpTime) },
                { InfoRow("취침 희망 시간", myInfo.sleepTime) }
            )
        )

        VerticalSpacer()

        InfoSection(
            items = listOf(
                { InfoRow("아침 희망 시간", myInfo.breakfastTime) },
                { InfoRow("점심 희망 시간", myInfo.lunchTime) },
                { InfoRow("저녁 희망 시간", myInfo.dinnerTime) }
            )
        )

        VerticalSpacer()

        InfoSection(
            items = listOf(
                { ToggleRow("알림 설정", myInfo.alarmEnabled) },
                { ToggleRow("배경음 설정", myInfo.bgSoundEnabled) },
                { ToggleRow("효과음 설정", myInfo.effectSoundEnabled) }
            )
        )

        VerticalSpacer(height = calculateBoxHeight())
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultMyInfoPreview() {
    MyInfoScreen()
}