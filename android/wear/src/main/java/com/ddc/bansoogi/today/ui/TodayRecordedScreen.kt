package com.ddc.bansoogi.today.ui

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
import com.ddc.bansoogi.common.ui.VerticalSpacer
import com.ddc.bansoogi.common.util.calculateBoxHeight
import com.ddc.bansoogi.today.data.ReportDto

@Composable
fun TodayRecordedScreen() {
    val report = ReportDto(
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

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BackgroundImage()

        OverlayBackground()

        TodayRecordedContent(report)
    }
}

@Composable
fun TodayRecordedContent(report: ReportDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 8.dp
            )
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("오늘 기록")

        InfoSection(
            items = listOf(
                { InfoRow("누워 있던 시간", report.lyingTime, unit = "분") },
                { InfoRow("앉아 있던 시간", report.sittingTime, unit = "분") },
                { InfoRow("휴대폰 사용 시간", report.phoneTime, unit = "분") }
            )
        )

        VerticalSpacer()

        InfoSection(
            items = listOf(
                { InfoRow("기상 이벤트", report.standupCount, unit = "회") },
                { InfoRow("스트레칭 이벤트", report.stretchCount, unit = "회") },
                { InfoRow("휴대폰 미사용 이벤트", report.phoneOffCount, unit = "회") }
            )
        )

        VerticalSpacer()

        InfoSection(
            items = listOf(
                { InfoRow("걸음 수", report.walkCount, unit = "걸음") },
                { InfoRow("런닝 시간", report.runTime, unit = "분") },
                { InfoRow("운동 시간", report.exerciseTime, unit = "분") },
                { InfoRow("계단 오르기", report.stairsClimbed, unit = "m") }
            )
        )

        VerticalSpacer(height = calculateBoxHeight())
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultTodayPreview() {
    TodayRecordedScreen()
}