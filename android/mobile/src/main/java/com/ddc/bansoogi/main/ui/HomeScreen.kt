package com.ddc.bansoogi.main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.manage.EggManagerModal
import com.ddc.bansoogi.main.ui.manage.HomeContent
import com.ddc.bansoogi.main.view.TodayRecordView
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import com.ddc.bansoogi.myInfo.view.MyInfoView
import java.time.LocalTime

@Preview
@Composable
fun HomeScreen() {
    var todayRecordDtoState = remember { mutableStateOf<TodayRecordDto?>(null) }
    var myInfoState = remember { mutableStateOf<MyInfoDto?>(null) }
    var showEggManager = remember { mutableStateOf(false) }
    var isInSleepRange = remember { mutableStateOf(false) }

    val myInfoController = remember {
        MyInfoController(object : MyInfoView {
            override fun displayMyInfo(myInfoDto: MyInfoDto) {
                myInfoState.value = myInfoDto
            }
        })
    }
    var todayRecordController = remember {
        TodayRecordController(object : TodayRecordView {
            override fun displayTodayRecord(todayRecordDto: TodayRecordDto) {
                todayRecordDtoState.value = todayRecordDto

                val now = LocalTime.now()

                // 취침시간 ~ 기상시간 사이라면 : isInSleepRange = true
                if (myInfoState.value!=null && !now.isBefore(LocalTime.parse(myInfoState.value?.sleepTime))
                    && !now.isAfter(LocalTime.parse(myInfoState.value?.wakeUpTime))) {
                    isInSleepRange.value = true;
                }

                // 이미 결산이 완료되었고, 취침 시간이 아니라면!
                if (todayRecordDto.isClosed) {
                    // TODO: 오늘 날짜 vs. todayRecord.createdAt 0일 혹은 1일 차이나는 지 확인하는 코드로 수정 필요!
                    if (isInSleepRange.value && 2<=1) {
                        showEggManager.value = false
                    }
                    else {
                        showEggManager.value = true
                    }
                }
            }

            override fun showEmptyState() {
                todayRecordDtoState.value = null
            }
        })
    }
    LaunchedEffect(Unit) {
        todayRecordController.initialize()
    }

    // Egg Manager 페이지 보여주기!
    if (showEggManager.value) {
        EggManagerModal(
            myInfo = myInfoState.value,
            onDismiss = {
                showEggManager.value = false
                // 새로운 TodayRecord 생성
                todayRecordController.renewTodayRecord()
            }
        )
    } else {
        todayRecordDtoState.value?.let { todayRecord ->
            HomeContent(todayRecord, todayRecordController, isInSleepRange.value)
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("로딩 중...", fontSize = 16.sp)
        }
    }
}
