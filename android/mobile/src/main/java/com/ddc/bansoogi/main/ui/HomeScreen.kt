package com.ddc.bansoogi.main.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ddc.bansoogi.calendar.controller.RecordedController
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.health.EnergyUtil
import com.ddc.bansoogi.common.wear.communication.state.HealthStateHolder
import com.ddc.bansoogi.main.controller.CharacterGetController
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.manage.HomeContent
import com.ddc.bansoogi.main.ui.util.InteractionUtil
import com.ddc.bansoogi.main.view.TodayRecordView
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import com.ddc.bansoogi.nearby.NearbyConnectionManager
import com.ddc.bansoogi.nearby.data.BansoogiFriend
import io.realm.kotlin.types.RealmInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import androidx.core.content.edit

// Extension to convert RealmInstant to LocalDate
fun RealmInstant.toLocalDate(): LocalDate {
    val instant = Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}

@Composable
fun HomeScreen(
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit,
    navController: NavController,
    // Added Nearby parameters
    isSearching: Boolean,
    toggleNearby: () -> Unit,
    peers: List<BansoogiFriend>,
    nearbyMgr: NearbyConnectionManager,
    userNickname: String,
    // Existing friend banner params
    showFriendBanner: Boolean = false,
    friendName: String = "",
    onDismissFriendBanner: () -> Unit = {}
) {
    val context = LocalContext.current
    var todayRecordDtoState = remember { mutableStateOf<TodayRecordDto?>(null) }
    var showEggManager = remember { mutableStateOf(false) }
    var isInSleepRange = remember { mutableStateOf(false) }

    // Controllers & state
    val myInfoState = remember { mutableStateOf<MyInfoDto?>(null) }
    val myInfoController = remember { MyInfoController() }
    val recordController = remember { RecordedController() }
    val todayRecordController = remember {
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
        if (myInfoController.isFirstUser(context)) showEggManager.value = true
    }

    LaunchedEffect(Unit) {
        onModalOpen()
        todayRecordController.initialize()
    }

    LaunchedEffect(Unit) {
        myInfoController.myInfoFlow().collect { dto -> myInfoState.value = dto }
    }

    LaunchedEffect(Unit) {
        HealthStateHolder.healthData?.let { health ->
            val energy = EnergyUtil.calculateEnergyOnce(health)
            todayRecordDtoState.value?.energyPoint = energy
        }
    }

    DisposableEffect(Unit) {
        onDispose { onModalClose() }
    }

    LaunchedEffect(todayRecordDtoState.value, myInfoState.value) {
        val myInfo = myInfoState.value ?: return@LaunchedEffect
        try {
            isInSleepRange.value = InteractionUtil.isInSleepRange(myInfo)
            val todayRecord = todayRecordDtoState.value ?: return@LaunchedEffect
            val createdDate = todayRecord.createdAt?.toLocalDate()
            if (createdDate != null) {
                val diffDays = abs(ChronoUnit.DAYS.between(RealmInstant.now().toLocalDate(), createdDate))
                if (todayRecord.isClosed) {
                    if (!(isInSleepRange.value && diffDays <= 1)) showEggManager.value = true
                } else {
                    if (!myInfoController.isFirstUser(context)) {
                        if ((isInSleepRange.value && diffDays <= 1) || (!isInSleepRange.value && diffDays > 0)) {
                            showEggManager.value = false
                            val prefs = context.getSharedPreferences("bansoogi_prefs", Context.MODE_PRIVATE)
                            val key = "egg_seen_${'$'}{LocalDate.now()}"
                            val alreadySeen = prefs.getBoolean(key, false)
                            if (!alreadySeen) {
                                if (CharacterGetController().canDrawCharacter()) {
                                    prefs.edit { putBoolean(key, true) }
                                    navController.navigate("character_get/${healthData.step.toInt()}/${healthData.floorsClimbed.toInt()}/${healthData.sleepData}/${healthData.exerciseTime ?: 0}")
                                } else {
                                    RecordedController().createRecordedReport(
                                        todayRecordDtoState.value!!,
                                        0,
                                        healthData.step.toInt(),
                                        healthData.floorsClimbed.toInt(),
                                        healthData.sleepData ?: 0,
                                        healthData.exerciseTime ?: 0
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
            Log.e("HomeScreen", "오류 발생: ${'$'}{e.message}", e)
        }
    }

    LaunchedEffect(showEggManager.value) {
        if (showEggManager.value) {
            navController.navigate(NavRoutes.EGGMANAGER)
            showEggManager.value = false
        }
    }

    // Render HomeContent with Nearby props
    todayRecordDtoState.value?.let { todayRecord ->
        HomeContent(
            todayRecordDto = todayRecord,
            todayRecordController = todayRecordController,
            isInSleepRange = isInSleepRange.value,
            healthData = healthData,
            isSearching = isSearching,
            toggleNearby = toggleNearby,
            peers = peers,
            nearbyMgr = nearbyMgr,
            userNickname = userNickname,
            showFriendBanner = showFriendBanner,
            friendName = friendName,
            onDismissFriendBanner = onDismissFriendBanner
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("로딩 중...", fontSize = 16.sp)
    }
}
