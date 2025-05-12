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
import io.realm.kotlin.types.RealmInstant
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun RealmInstant.toLocalDate(): LocalDate {
    val instant = Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}

@Preview
@Composable
fun HomeScreen() {
    var todayRecordDtoState = remember { mutableStateOf<TodayRecordDto?>(null) }
    var showEggManager = remember { mutableStateOf(false) }
    var isInSleepRange = remember { mutableStateOf(false) }

    val myInfoState = remember { mutableStateOf<MyInfoDto?>(null) }   // 기존 변수 그대로 사용
    val myInfoController = remember { MyInfoController() }

    LaunchedEffect(Unit) {
        myInfoController.myInfoFlow().collect { dto ->
            myInfoState.value = dto
        }
    }

    var todayRecordController = remember {
        TodayRecordController(object : TodayRecordView {
            override fun displayTodayRecord(todayRecordDto: TodayRecordDto) {
                todayRecordDtoState.value = todayRecordDto
            }

            override fun showEmptyState() {
                todayRecordDtoState.value = null
            }
        })
    }
    LaunchedEffect(Unit) {
        todayRecordController.initialize()

        if (todayRecordDtoState.value == null) return@LaunchedEffect

        val now = LocalTime.now()

        // 취침시간 ~ 기상시간 사이라면 : isInSleepRange = true
        if (myInfoState.value!=null && !now.isBefore(LocalTime.parse(myInfoState.value?.sleepTime))
            && !now.isAfter(LocalTime.parse(myInfoState.value?.wakeUpTime))) {
            isInSleepRange.value = true;
        }

        // 이미 결산이 완료되었고, 취침 시간이 아니라면!
        var diffDays = ChronoUnit.DAYS.between(RealmInstant.now().toLocalDate(),
            todayRecordDtoState.value?.createdAt?.toLocalDate()
        )
        if (todayRecordDtoState.value?.isClosed == true) {
            if (!(isInSleepRange.value && diffDays<=1)) {
                showEggManager.value = true
            }
        } else {
            // 결산이 완료되진 않았는데, 취침시간이라면 => 결산 + isClosed 갱신
            if (isInSleepRange.value && diffDays<=1) {
                showEggManager.value = false

                todayRecordController.updateIsClosed()
            }
        }
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
