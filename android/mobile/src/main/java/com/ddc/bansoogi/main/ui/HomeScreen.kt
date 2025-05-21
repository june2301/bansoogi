package com.ddc.bansoogi.main.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.ddc.bansoogi.calendar.controller.RecordedController
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.controller.CharacterGetController
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.manage.EggManagerModal
import com.ddc.bansoogi.main.ui.manage.HomeContent
import com.ddc.bansoogi.main.ui.util.InteractionUtil
import com.ddc.bansoogi.main.view.TodayRecordView
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import io.realm.kotlin.types.RealmInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import androidx.core.content.edit
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.util.health.EnergyUtil
import com.ddc.bansoogi.common.wear.communication.state.HealthStateHolder

fun RealmInstant.toLocalDate(): LocalDate {
    val instant = Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}

@Composable
fun HomeScreen(
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
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
        }, context = context)
    }

    LaunchedEffect(Unit) {
        if (myInfoController.isFirstUser(context)) {
            showEggManager.value = true
        }
    }

    LaunchedEffect(Unit) {
        onModalOpen()
        todayRecordController.initialize()
    }

    LaunchedEffect(Unit) {
        myInfoController.myInfoFlow().collect { dto ->
            myInfoState.value = dto
        }
    }

    // 헬스 데이터 호출해서 점수 계산
    LaunchedEffect(Unit) {
        val health =  HealthStateHolder.healthData
        if (health != null) {
            val energy = EnergyUtil.calculateEnergyOnce(health)
            todayRecordDtoState.value?.energyPoint = energy
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onModalClose()
        }
    }

    // todayRecord와 myInfo 변경에 반응
    LaunchedEffect(todayRecordDtoState.value, myInfoState.value) {
        val myInfo = myInfoState.value ?: return@LaunchedEffect

        try {
            isInSleepRange.value = InteractionUtil.isInSleepRange(myInfo)

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
                    if (!myInfoController.isFirstUser(context)) {
                        // 결산이 완료되진 않았는데, 취침시간이거나, 이미 지난 날짜라면 => 결산 + isClosed 갱신
                        if ((isInSleepRange.value && diffDays <= 1) || (!isInSleepRange.value && diffDays > 0)) {
                            showEggManager.value = false

                            // CharacterGetScreen으로 이동 (단, 오늘 이미 본 적 없다면)
                            val prefs = context.getSharedPreferences("bansoogi_prefs", Context.MODE_PRIVATE)
                            val key = "egg_seen_${LocalDate.now()}"
                            val alreadySeen = prefs.getBoolean(key, false)

                            if (!alreadySeen) {
                                if (CharacterGetController().canDrawCharacter()) {
                                    prefs.edit() { putBoolean(key, true) }
                                    navController.navigate("character_get/${healthData.step.toInt()}/${healthData.floorsClimbed.toInt()}/${healthData.sleepData}/${healthData.exerciseTime?:0}")
                                }
                                // 점수가 80점 미만인 경우!
                                else {
                                    RecordedController().createRecordedReport(
                                        todayRecordDtoState.value!!,
                                        0,
                                        healthData.step.toInt(),
                                        healthData.floorsClimbed.toInt(),
                                        healthData.sleepData?:0,
                                        healthData.exerciseTime?:0
                                    )
                                }
                            }
                        }
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
    LaunchedEffect(showEggManager.value) {
        if (showEggManager.value) {
            navController.navigate(NavRoutes.EGGMANAGER)
            showEggManager.value = false
        }
    }

    // 6) 화면 렌더링: 항상 HomeContent (또는 로딩)
    todayRecordDtoState.value?.let { todayRecord ->
        HomeContent(
            todayRecord,
            todayRecordController,
            isInSleepRange.value,
            healthData
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("로딩 중...", fontSize = 16.sp)
    }
}
