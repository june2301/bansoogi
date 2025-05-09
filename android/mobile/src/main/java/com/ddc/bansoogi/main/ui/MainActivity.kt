package com.ddc.bansoogi.main.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.ddc.bansoogi.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ddc.bansoogi.common.navigation.AppNavGraph
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.CommonNavigationBar
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.health.Permissions
import com.ddc.bansoogi.common.util.health.RealTimeHealthDataManager
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val activityContext = this
    private lateinit var healthDataStore: HealthDataStore
    private lateinit var healthDataManager: RealTimeHealthDataManager

    private var healthData by mutableStateOf(CustomHealthData(0L, 0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthDataStore = HealthDataService.getStore(activityContext)
//        var result = setupHealthData(lifecycleScope, healthDataStore)

        setupHealthPermissions()

        setContent {
            MainScreen(healthData)
        }
    }

    /**
     * setupHealthPermission: health 앱에 대한 권한이 있는 지 체크
     */
    private fun setupHealthPermissions() {
        lifecycleScope.launch {
            try {
                val grantedPermissions = healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS)

                if (grantedPermissions.size != Permissions.PERMISSIONS.size) {
                    val result = healthDataStore.requestPermissions(Permissions.PERMISSIONS, this@MainActivity)
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
                // 데이터가 업데이트될 때마다 UI 상태 갱신
                healthData = data
                Log.d("STEPS", "Updated Step Goal: ${data.step}")
                Log.d("TODAY STEP", "Updated Today Step: ${data.stepGoal}")
            }
        }

        // 데이터 수집 시작
        healthDataManager.startCollecting()
    }

    override fun onResume() {
        super.onResume()
        // 앱이 전면에 올 때마다 수동 갱신
        if (::healthDataManager.isInitialized) {
            healthDataManager.refreshData()
        }
    }

    override fun onPause() {
        super.onPause()
        // 배터리 절약을 위해 앱이 백그라운드로 갈 때 수집 속도 줄이기 (옵션)
        if (::healthDataManager.isInitialized) {
            healthDataManager.setUpdateInterval(60000) // 1분으로 증가
        }
    }

    override fun onDestroy() {
        // 액티비티 종료 시 수집 중지
        if (::healthDataManager.isInitialized) {
            healthDataManager.stopCollecting()
        }
        super.onDestroy()
    }

//    fun setupHealthData(lifecycleScope: LifecycleCoroutineScope, healthDataStore: HealthDataStore): CustomHealthData {
//        var stepGoal = 0
//        var todaySteps = 0L
//        lifecycleScope.launch {
//            try {
//                val grantedPermissions = healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS)
//
//                if (grantedPermissions.size != Permissions.PERMISSIONS.size) {
//                    val result = healthDataStore.requestPermissions(Permissions.PERMISSIONS, this@MainActivity)
//                }
//
//                // 권한이 허용된 후에만 데이터 읽기 시도
//                if (healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS).size == Permissions.PERMISSIONS.size) {
//                    stepGoal = readLastStepGoal(healthDataStore)
//                    todaySteps = readStepData(healthDataStore)
//                    Log.d("STEPS", "Step Goal: $stepGoal")
//                    Log.d("TODAY STEP", "Today Step: $todaySteps")
//                }
//            } catch (e: Exception) {
//                Log.e("STEPS", "Error with Samsung Health: ${e.message}", e)
//            }
//        }
//        return CustomHealthData(todaySteps, stepGoal)
//    }
}

@Composable
fun MainScreen(healthData: CustomHealthData) {
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
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                CommonNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(NavRoutes.HOME) {
                                    saveState = true
                                    // HOME으로 이동할 경우, 백스택 완전히 비우기
                                    inclusive = (route == NavRoutes.HOME)
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
            )
        }
    }
}