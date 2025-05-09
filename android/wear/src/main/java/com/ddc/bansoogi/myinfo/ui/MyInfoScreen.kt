package com.ddc.bansoogi.myinfo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.common.mobile.communication.sender.MobileMyInfoSender
import com.ddc.bansoogi.common.ui.BackgroundImage
import com.ddc.bansoogi.common.ui.InfoRow
import com.ddc.bansoogi.common.ui.InfoSection
import com.ddc.bansoogi.common.ui.OverlayBackground
import com.ddc.bansoogi.common.ui.ScreenHeader
import com.ddc.bansoogi.common.ui.ToggleRow
import com.ddc.bansoogi.common.ui.VerticalSpacer
import com.ddc.bansoogi.common.util.calculateBoxHeight
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import com.ddc.bansoogi.myinfo.data.store.getCachedMyInfo
import com.ddc.bansoogi.myinfo.state.MyInfoStateHolder
import kotlinx.coroutines.flow.first

@Composable
fun MyInfoScreen() {
    val context = LocalContext.current

    // 초기 로컬 데이터 로딩
    LaunchedEffect(Unit) {
        // 초기에는 로컬에서 데이터를 호출
        val cached = getCachedMyInfo(context).first()
        MyInfoStateHolder.update(cached)

        // 모바일로 데이터 송신 요청을 전송
        MobileMyInfoSender.send(context)
    }

    val myInfo = MyInfoStateHolder.myInfoDto

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BackgroundImage()

        OverlayBackground()

        if (myInfo != null) {
            MyInfoContent(myInfo)
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("데이터 수신 대기 중...")
            }
        }
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
                { ToggleRow("알림 설정", myInfo.notificationEnabled) },
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