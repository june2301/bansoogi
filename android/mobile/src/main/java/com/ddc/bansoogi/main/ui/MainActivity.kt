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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ddc.bansoogi.common.navigation.AppNavGraph
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.CommonNavigationBar
import com.ddc.bansoogi.main.util.health.Permissions
import com.ddc.bansoogi.main.util.health.readLastStepGoal
import com.ddc.bansoogi.main.util.health.readStepData
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val activityContext = this
    private lateinit var healthDataStore: HealthDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthDataStore = HealthDataService.getStore(activityContext)

        setContent {
            MainScreen()
        }

        setupHealthData(lifecycleScope, healthDataStore)
    }
    fun setupHealthData(lifecycleScope: LifecycleCoroutineScope, healthDataStore: HealthDataStore) {
        lifecycleScope.launch {
            try {
                val grantedPermissions = healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS)

                if (grantedPermissions.size != Permissions.PERMISSIONS.size) {
                    val result = healthDataStore.requestPermissions(Permissions.PERMISSIONS, this@MainActivity)
                }

                // 권한이 허용된 후에만 데이터 읽기 시도
                if (healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS).size == Permissions.PERMISSIONS.size) {
                    val stepGoal = readLastStepGoal(healthDataStore)
                    val todaySteps = readStepData(healthDataStore)
                    Log.d("STEPS", "Step Goal: $stepGoal")
                    Log.d("TODAY STEP", "Today Step: $todaySteps")
                }
            } catch (e: Exception) {
                Log.e("STEPS", "Error with Samsung Health: ${e.message}", e)
            }
        }
    }
}

@Composable
fun MainScreen() {
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
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}