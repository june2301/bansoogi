package com.ddc.bansoogi.main.ui

import android.os.Build
import android.os.Bundle
import android.text.style.BackgroundColorSpan
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.navigation.AppNavGraph
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.activity.BaseActivity
import com.ddc.bansoogi.common.ui.CommonNavigationBar
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.health.Permissions
import com.ddc.bansoogi.common.util.health.RealTimeHealthDataManager
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.launch

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

        setContent {
            MainScreen(
                healthData,
                onModalOpen = { startHealthDataUpdates() },
                onModalClose = { stopHealthDataUpdates() }
            )
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
            healthDataManager.refreshData() // 즉시 한 번 갱신
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

@Composable
fun BansoogiNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavRoutes.HOME to R.drawable.ic_home,
        NavRoutes.COLLECTION to R.drawable.ic_home,
        NavRoutes.CALENDAR to R.drawable.ic_home,
        NavRoutes.MYINFO to R.drawable.ic_home
    )

    Surface(
        color = Color.Transparent,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (route, iconRes) ->
                val isSelected = route == currentRoute

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = tween(durationMillis = 200),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFFDE68A) else Color.Transparent)
                        .clickable { onNavigate(route) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = route,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                    )
                }
            }
        }
    }
}
