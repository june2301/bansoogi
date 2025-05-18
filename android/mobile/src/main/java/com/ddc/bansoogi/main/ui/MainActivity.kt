package com.ddc.bansoogi.main.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ddc.bansoogi.common.foreground.ForegroundUtil
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.ui.util.CalendarUtils
import com.ddc.bansoogi.common.navigation.AppNavGraph
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.activity.BaseActivity
import com.ddc.bansoogi.common.ui.component.BansoogiNavigationBar
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.health.Permissions
import com.ddc.bansoogi.common.util.health.RealTimeHealthDataManager
import com.ddc.bansoogi.main.controller.TodayHealthDataController
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : BaseActivity() {
    val activityContext = this
    private lateinit var healthDataStore: HealthDataStore
    private lateinit var healthDataManager: RealTimeHealthDataManager

    private var healthData by mutableStateOf(CustomHealthData(0L, 0, 0.0f, 0, 0))

    companion object {
        private const val UPDATE_INTERVAL = 10000L // 포그라운드: 10초
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthDataStore = HealthDataService.getStore(activityContext)

        setupHealthPermissions()

        val today = LocalDate.now()
        val todayFormatted = CalendarUtils.toFormattedDateString(today, today.dayOfMonth)
        TodayHealthDataController().initialize(todayFormatted)
        setContent {
            MainScreen(
                healthData,
                onModalOpen = { startHealthDataUpdates() },
                onModalClose = { stopHealthDataUpdates() }
            )
        }
        if (::healthDataManager.isInitialized) {
            healthDataManager.refreshData() // 초기 데이터 즉시 로드
        }
    }

    override fun onResume() {
        super.onResume()

        if (!ForegroundUtil.isServiceRunning()) {
            ForegroundUtil.startForegroundService(activityContext)
        }
    }

    /**
     * setupHealthPermission: health 앱에 대한 권한이 있는 지 체크
     */
    private fun setupHealthPermissions() {
        lifecycleScope.launch {
            try {
                val grantedPermissions =
                    healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS)

                if (grantedPermissions.size != Permissions.PERMISSIONS.size) {
                    val result = healthDataStore.requestPermissions(
                        Permissions.PERMISSIONS,
                        this@MainActivity
                    )
                }

                // 모든 권한이 있으면 실시간 데이터 매니저 초기화 및 시작
                if (healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS).size == Permissions.PERMISSIONS.size) {
                    initializeHealthDataManager()
                }
            } catch (e: Exception) {
                Log.e("STEPS", "Error with Samsung Health permissions: ${e.message}", e)
            }
        }
    }

    private fun initializeHealthDataManager() {
        // 실시간 데이터 매니저 초기화
        healthDataManager = RealTimeHealthDataManager(healthDataStore)

        // Flow 수집 시작
        lifecycleScope.launch {
            healthDataManager.healthData.collect { data ->
                healthData = data
            }
        }
    }

    // 모달이 열릴 때 호출될 메서드
    fun startHealthDataUpdates() {
        if (::healthDataManager.isInitialized) {
            healthDataManager.setUpdateInterval(UPDATE_INTERVAL)
//            healthDataManager.refreshData() // 즉시 한 번 갱신
            healthDataManager.startCollecting() // 데이터 수집 시작
        }
    }

    // 모달이 닫힐 때 호출될 메서드
    fun stopHealthDataUpdates() {
        if (::healthDataManager.isInitialized) {
            healthDataManager.stopCollecting() // 데이터 수집 중지
        }
    }

    override fun onDestroy() {
        // 액티비티 종료 시 수집 중지
        if (::healthDataManager.isInitialized) {
            healthDataManager.stopCollecting()
        }
        super.onDestroy()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen(
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute = when {
        currentDestination?.hierarchy?.any { it.route == NavRoutes.HOME } == true -> NavRoutes.HOME
        currentDestination?.hierarchy?.any { it.route == NavRoutes.COLLECTION } == true -> NavRoutes.COLLECTION
        currentDestination?.hierarchy?.any { it.route == NavRoutes.CALENDAR } == true -> NavRoutes.CALENDAR
        currentDestination?.hierarchy?.any { it.route == NavRoutes.MYINFO } == true -> NavRoutes.MYINFO
        currentDestination?.hierarchy?.any { it.route == NavRoutes.MYINFOUPDATE } == true -> NavRoutes.MYINFO
        else -> NavRoutes.HOME
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_sunny_sky),
            contentDescription = "배경 하늘",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BansoogiNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(NavRoutes.HOME) {
                                    saveState = true
                                    inclusive = false
                                }
                                // 중복 방지
                                launchSingleTop = true
                                // 상태 저장
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            AppNavGraph(
                navController = navController,
                modifier = Modifier.padding(paddingValues),
                healthData = healthData,
                onModalOpen = onModalOpen,
                onModalClose = onModalClose
            )
        }
    }
}