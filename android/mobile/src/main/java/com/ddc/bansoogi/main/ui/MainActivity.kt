package com.ddc.bansoogi.main.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.ddc.bansoogi.common.wear.communication.state.HealthStateHolder
import com.ddc.bansoogi.main.bluetooth.BansoogiBluetoothManager
import com.ddc.bansoogi.main.bluetooth.BluetoothFloatingButton
import com.ddc.bansoogi.main.bluetooth.BluetoothPermissionDialog
import com.ddc.bansoogi.main.bluetooth.BluetoothPermissionManager
import com.ddc.bansoogi.main.bluetooth.BluetoothStatusMessage
import com.ddc.bansoogi.main.bluetooth.FriendFoundNotification
import com.ddc.bansoogi.main.controller.TodayHealthDataController
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : BaseActivity() {
    val activityContext = this
    private var isFirstUser = false
    private lateinit var healthDataStore: HealthDataStore
    private lateinit var healthDataManager: RealTimeHealthDataManager

    private var healthData by mutableStateOf(CustomHealthData(0L, 0, 0.0f, 0, 0))

    companion object {
        private const val UPDATE_INTERVAL = 10000L // 포그라운드: 10초
    }

    // 블루투스 관련 변수들
    private lateinit var bansoogiBluetoothManager: BansoogiBluetoothManager
    private var bluetoothStatus by mutableStateOf("")
    private var isBluetoothSearching by mutableStateOf(false)
    private var showBluetoothDialog by mutableStateOf(false)

    // 친구 발견 알림 관련
    private var showFriendFoundNotification by mutableStateOf(false)
    private var currentFriendName by mutableStateOf("")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthDataStore = HealthDataService.getStore(activityContext)
        val prefs = getSharedPreferences("bansoogi_prefs", MODE_PRIVATE)
        isFirstUser = prefs.getBoolean("isFirstUser", false)

        setupHealthPermissions()

        val today = LocalDate.now()
        val todayFormatted = CalendarUtils.toFormattedDateString(today, today.dayOfMonth)
        TodayHealthDataController().initialize(todayFormatted)

        bansoogiBluetoothManager = BansoogiBluetoothManager(this)

        // 친구 발견 콜백 설정
        bansoogiBluetoothManager.onFriendFound = { friendName ->
            currentFriendName = friendName
            showFriendFoundNotification = true
            Log.d("MainActivity", "새로운 반숙이 친구 발견: $friendName")
        }

        // 스캔 상태 관찰
        lifecycleScope.launch {
            bansoogiBluetoothManager.isScanning.collect { scanning ->
                isBluetoothSearching = scanning
            }
        }

        setContent {
            MainScreen(
                healthData = healthData,
                onModalOpen = { startHealthDataUpdates() },
                onModalClose = { stopHealthDataUpdates() },
                isFirstUser = isFirstUser,
                bluetoothManager = bansoogiBluetoothManager,
                bluetoothStatus = bluetoothStatus,
                isBluetoothSearching = isBluetoothSearching,
                showBluetoothDialog = showBluetoothDialog,
                onBluetoothButtonClick = { handleBluetoothTrigger() },
                onBluetoothDialogDismiss = { showBluetoothDialog = false },
                showFriendFoundNotification = showFriendFoundNotification,
                currentFriendName = currentFriendName,
                onDismissFoundNotification = { showFriendFoundNotification = false }
            )
        }
        if (::healthDataManager.isInitialized) {
            healthDataManager.refreshData() // 초기 데이터 즉시 로드
        }
    }

    private fun handleBluetoothTrigger() {
        when {
            !bansoogiBluetoothManager.isBluetoothSupported() -> {
                bluetoothStatus = "이 기기는 블루투스를 지원하지 않아요 😢"
            }
            !bansoogiBluetoothManager.hasAllPermissions() -> {
                showBluetoothDialog = true
            }
            !bansoogiBluetoothManager.isBluetoothEnabled() -> {
                requestBluetoothEnable()
            }
            else -> {
                // 친구 찾기 트리거 실행
                bluetoothStatus = bansoogiBluetoothManager.triggerFriendSearch()

                // 상태 메시지 자동 사라짐
                Handler(Looper.getMainLooper()).postDelayed({
                    bluetoothStatus = ""
                }, 3000)
            }
        }
    }

    private fun requestBluetoothEnable() {
        if (bansoogiBluetoothManager.hasAllPermissions()) {
            try {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1002)
            } catch (e: SecurityException) {
                bluetoothStatus = "블루투스 권한이 필요해요!"
                isBluetoothSearching = false
            }
        } else {
            bluetoothStatus = "블루투스 권한이 필요해요!"
            showBluetoothDialog = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1002) { // 블루투스 활성화 요청
            if (resultCode == RESULT_OK) {
                if (bansoogiBluetoothManager.hasAllPermissions()) {
                    bansoogiBluetoothManager.initializeAfterPermissionGranted()
                    handleBluetoothTrigger()
                } else {
                    bluetoothStatus = "블루투스 권한이 필요해요!"
                    showBluetoothDialog = true
                }
            } else {
                bluetoothStatus = "블루투스를 켜야 친구들을 찾을 수 있어요!"
            }
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

                // 모바일로 전송을 위한 객체 관리
                HealthStateHolder.update(healthData)
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
    onModalClose: () -> Unit,
    isFirstUser: Boolean,
    bluetoothManager: BansoogiBluetoothManager,
    bluetoothStatus: String,
    isBluetoothSearching: Boolean,
    showBluetoothDialog: Boolean,
    onBluetoothButtonClick: () -> Unit,
    onBluetoothDialogDismiss: () -> Unit,
    showFriendFoundNotification: Boolean,
    currentFriendName: String,
    onDismissFoundNotification: () -> Unit
) {
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            onBluetoothButtonClick() // 권한이 허용되면 다시 시도
        }
        onBluetoothDialogDismiss()
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute = when {
        currentDestination?.hierarchy?.any { it.route == NavRoutes.HOME } == true -> NavRoutes.HOME
        currentDestination?.hierarchy?.any { it.route == NavRoutes.COLLECTION } == true -> NavRoutes.COLLECTION
        currentDestination?.hierarchy?.any { it.route == NavRoutes.CALENDAR } == true -> NavRoutes.CALENDAR
        currentDestination?.hierarchy?.any { it.route == NavRoutes.MYINFO } == true -> NavRoutes.MYINFO
        currentDestination?.hierarchy?.any { it.route == NavRoutes.MYINFOUPDATE } == true -> NavRoutes.MYINFO
        currentDestination?.hierarchy?.any { it.route == NavRoutes.EGGMANAGER } == true -> NavRoutes.EGGMANAGER
        else -> NavRoutes.HOME
    }

    var currentBackgroundResId by remember{ mutableIntStateOf(R.drawable.background_sunny_sky) }

    LaunchedEffect(BansoogiStateHolder.state) {
        currentBackgroundResId = BansoogiStateHolder.background()
    }

    val lightPosition = remember { Animatable(initialValue = -1000f) }
    LaunchedEffect(Unit) {
        // 빛이 위에서 아래로 내려오는 애니메이션
        lightPosition.animateTo(
            targetValue = 2000f,
            animationSpec = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            )
        )
    }

    val discoveredFriends by bluetoothManager.discoveredFriends.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentBackgroundResId,
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 1000),
                    initialOffsetX = { -it } // 위에서 아래로 등장
                ) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 1000),
                            targetOffsetX = { it }   // 아래로 밀려서 퇴장
                        )
            }

        ) { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (currentRoute != NavRoutes.EGGMANAGER) {
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
            },
            // 플로팅 액션 버튼으로 블루투스 버튼 추가
            floatingActionButton = {
                if (currentRoute == NavRoutes.HOME) {
                    BluetoothFloatingButton(
                        isSearching = isBluetoothSearching,
                        onClick = onBluetoothButtonClick
                    )
                }
            }
        ) { paddingValues ->
            val contentMod = if (currentRoute == NavRoutes.EGGMANAGER) {
                Modifier.fillMaxSize()
            } else {
                Modifier.padding(paddingValues)
            }
            AppNavGraph(
                navController = navController,
                modifier = contentMod,
                healthData = healthData,
                onModalOpen = onModalOpen,
                onModalClose = onModalClose,
                isFirstUser = isFirstUser
            )
            // 블루투스 상태 메시지 표시
            AnimatedVisibility(
                visible = bluetoothStatus.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                BluetoothStatusMessage(
                    message = bluetoothStatus,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // 친구 발견 알림
            FriendFoundNotification(
                friendName = currentFriendName,
                isVisible = showFriendFoundNotification,
                onDismiss = onDismissFoundNotification,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 발견된 친구 목록 표시 (홈 화면에서만)
            if (currentRoute == NavRoutes.HOME && discoveredFriends.isNotEmpty()) {
                val friendsToShow = discoveredFriends.take(3) // 최대 3명만 표시

                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .widthIn(max = 300.dp)
                ) {
                    items(
                        count = friendsToShow.size,
                        key = { index -> friendsToShow[index].deviceAddress }
                    ) { index ->
                        val friend = friendsToShow[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.9f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "🥚 ${friend.nickname}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "신호세기: ${friend.rssi}dBm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    if (discoveredFriends.size > 3) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.7f)
                                )
                            ) {
                                Text(
                                    text = "... 외 ${discoveredFriends.size - 3}명",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // 블루투스 권한 요청 다이얼로그
            if (showBluetoothDialog) {
                BluetoothPermissionDialog(
                    onConfirm = {
                        bluetoothPermissionLauncher.launch(
                            BluetoothPermissionManager.getRequiredPermissions()
                        )
                    },
                    onDismiss = onBluetoothDialogDismiss
                )
            }
        }
    }
}