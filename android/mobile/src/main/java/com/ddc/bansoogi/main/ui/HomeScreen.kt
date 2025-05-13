package com.ddc.bansoogi.main.ui

import android.util.Log
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
import com.ddc.bansoogi.calendar.controller.RecordedController
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.manage.EggManagerModal
import com.ddc.bansoogi.main.ui.manage.HomeContent
import com.ddc.bansoogi.main.ui.util.isInSleepRange
import com.ddc.bansoogi.main.view.TodayRecordView
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import io.realm.kotlin.types.RealmInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

fun RealmInstant.toLocalDate(): LocalDate {
    val instant = Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}

@Composable
fun HomeScreen(
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit,
) {
    var todayRecordDtoState = remember { mutableStateOf<TodayRecordDto?>(null) }
    var showEggManager = remember { mutableStateOf(false) }
    var isInSleepRange = remember { mutableStateOf(false) }

    val myInfoState = remember { mutableStateOf<MyInfoDto?>(null) }
    val myInfoController = remember { MyInfoController() }
    var recordController = remember { RecordedController() }

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
    }

    LaunchedEffect(Unit) {
        myInfoController.myInfoFlow().collect { dto ->
            myInfoState.value = dto
        }
    }

    // todayRecord와 myInfo 변경에 반응
    LaunchedEffect(todayRecordDtoState.value, myInfoState.value) {
        val myInfo = myInfoState.value ?: return@LaunchedEffect

        try {
            isInSleepRange.value = isInSleepRange(myInfo)

            val todayRecord = todayRecordDtoState.value ?: return@LaunchedEffect

            val createdDate = todayRecord.createdAt?.toLocalDate()
            if (createdDate != null) {
                val diffDays =
                    abs(ChronoUnit.DAYS.between(RealmInstant.now().toLocalDate(), createdDate))

                if (todayRecord.isClosed) {
                    // 이미 결산이 완료되었고, 취침 시간이 아니라면!
                    if (!(isInSleepRange.value && diffDays <= 1)) {
                        showEggManager.value = true
                    }
                } else {
                    // 결산이 완료되진 않았는데, 취침시간이거나, 이미 지난 날짜라면 => 결산 + isClosed 갱신
                    if ((isInSleepRange.value && diffDays <= 1) || (!isInSleepRange.value && diffDays > 0)) {
                        showEggManager.value = false
                        todayRecordController.updateIsClosed()
                        recordController.createRecordedReport(
                            todayRecord,
                            1,
                            healthData.step.toInt(),
                            0,
                            0,
                            healthData.floorsClimbed.toInt()
                        )
                    }
                }
            } else {
                Log.d("HomeScreen", "생성 날짜가 null입니다")
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "오류 발생: ${e.message}", e)
        }
    }

    // Egg Manager 페이지 보여주기!
    if (showEggManager.value) {
        EggManagerModal(
            myInfo = myInfoState.value,
            onDismiss = {
                showEggManager.value = false
                todayRecordController.renewTodayRecord()
            }
        )
    } else {
        todayRecordDtoState.value?.let { todayRecord ->
            HomeContent(
                todayRecord,
                todayRecordController,
                isInSleepRange.value,
                healthData,
                onModalOpen = onModalOpen,
                onModalClose = onModalClose
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("로딩 중...", fontSize = 16.sp)
        }
    }
}
