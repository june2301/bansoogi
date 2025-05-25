package com.ddc.bansoogi.main.ui

/* ───────── Android / Kotlin ───────── */
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.ui.util.CalendarUtils
import com.ddc.bansoogi.common.foreground.ForegroundUtil
import com.ddc.bansoogi.common.navigation.AppNavGraph
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.activity.BaseActivity
import com.ddc.bansoogi.common.ui.component.BansoogiNavigationBar
import com.ddc.bansoogi.common.util.health.*
import com.ddc.bansoogi.common.wear.communication.state.HealthStateHolder
import com.ddc.bansoogi.main.controller.TodayHealthDataController
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.nearby.NearbyConnectionManager
import com.ddc.bansoogi.nearby.NearbyPermissionManager
import com.ddc.bansoogi.nearby.data.BansoogiFriend
import com.ddc.bansoogi.nearby.ui.FriendFoundNotification
import com.ddc.bansoogi.nearby.ui.NearbyFloatingButton
import com.ddc.bansoogi.person.data.local.PersonDataSource
import com.ddc.bansoogi.person.data.model.PersonModel
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

/* ─────────────────────────────────── */

class MainActivity : BaseActivity() {

    /* ---------- Health ---------- */
    private lateinit var healthStore: HealthDataStore
    private lateinit var healthMgr: RealTimeHealthDataManager
    private var healthData by mutableStateOf(CustomHealthData(0L, 0, 0f, 0, 0))

    /* ---------- Nearby ---------- */
    private lateinit var nearbyMgr: NearbyConnectionManager
    private var isSearching by mutableStateOf(false)
    private lateinit var nearbyPermLauncher: ActivityResultLauncher<Array<String>>

    /* ---------- States ---------- */
    private var showFriendBanner by mutableStateOf(false)
    private var friendName       by mutableStateOf("")
    private var showPermDialog   by mutableStateOf(false)
    private var isFirstUser      by mutableStateOf(false)

    /* ---------- Nickname --------- */
    private var userNickname by mutableStateOf("엄윤준")

    private val UPDATE_INTERVAL  = 10_000L

    /* =================================================================== */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Samsung Health 권한 */
        healthStore = HealthDataService.getStore(this)
        checkHealthPermissions()

        /* 첫 사용자 여부 */
        isFirstUser = getSharedPreferences("bansoogi_prefs", MODE_PRIVATE)
            .getBoolean("isFirstUser", false)

        /* 오늘 데이터 초기화 */
        TodayHealthDataController().initialize(
            CalendarUtils.toFormattedDateString(LocalDate.now(), LocalDate.now().dayOfMonth)
        )

        /* ② Nearby 런타임 권한 런처 등록 */
        nearbyPermLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            val allGranted = granted.values.all { it }
            if (allGranted) {
                toggleNearby()       // 권한 승인 후 바로 탐색 시작
            } else {
                Log.e("MainActivity",(  "근거리 탐색 권한이 거부되었습니다."))
            }
        }

        /* Nearby 매니저 */
        nearbyMgr = NearbyConnectionManager(this).apply {
            lifecycleScope.launch {
                peers.collect { list ->
                    if (list.isNotEmpty()) {
                        friendName = list.last().nickname
                        showFriendBanner = true
                    }
                }
            }
        }

        /* Compose */
        setContent {
            MainScreen(
                healthData       = healthData,
                onModalOpen      = { startHealthUpdates() },
                onModalClose     = { stopHealthUpdates() },
                isFirstUser      = isFirstUser,
                isSearching      = isSearching,
                toggleNearby     = { toggleNearby() },
                nearbyMgr        = nearbyMgr,
                showFriendBanner = showFriendBanner,
                friendName       = friendName,
                dismissBanner    = { showFriendBanner = false },
                showPermDialog   = showPermDialog,
                onPermDismiss    = { showPermDialog = false }
            )
        }
        /* 초기 권한이 이미 있었다면 즉시 한 번 로드 */
        if (::healthMgr.isInitialized) healthMgr.refreshData()
    }

    /* ------------------------- Health 권한 ------------------------ */
    private fun checkHealthPermissions() {
        lifecycleScope.launch {
            try {
                val granted = healthStore.getGrantedPermissions(Permissions.PERMISSIONS)

                if (granted.size == Permissions.PERMISSIONS.size) {
                    initHealthMgr()                    // ✅ 이미 허용된 경우 바로 초기화
                } else {
                    healthStore.requestPermissions(Permissions.PERMISSIONS, this@MainActivity)
                    // 재확인 → 모두 획득되면 초기화
                    if (healthStore.getGrantedPermissions(Permissions.PERMISSIONS).size ==
                        Permissions.PERMISSIONS.size) {
                        initHealthMgr()
                    }
                }
            } catch (e: Exception) {
                Log.e("STEPS", "Error with Samsung Health permissions: ${e.message}", e)
            }
        }
    }

    private fun initHealthMgr() {
        healthMgr = RealTimeHealthDataManager(healthStore)
        lifecycleScope.launch {
            healthMgr.healthData.collect { data ->
                healthData = data
                HealthStateHolder.update(data)
            }
        }
        /* ▶ 권한 획득 직후 즉시 1회 데이터 로드 */
        try {
            healthMgr.refreshData()
        } catch (e: Exception) {
            Log.e("MainActivity", "refreshData() error: ${e.message}", e)
        }
    }
    private fun startHealthUpdates() {
        if (::healthMgr.isInitialized) {
            healthMgr.setUpdateInterval(UPDATE_INTERVAL)
            healthMgr.startCollecting()
        }
    }
    private fun stopHealthUpdates() {
        if (::healthMgr.isInitialized) healthMgr.stopCollecting()
    }

    /* ------------------------- Nearby 토글 ------------------------ */
    /* 요청-부족 → 다이얼로그 표시 */
    private fun toggleNearby() {
        if (!isSearching) {                      // “탐색 시작” 직전에만 권한 검사
            val lacking = requiredNearbyPermissions()
                .filterNot { perm ->
                    ContextCompat.checkSelfPermission(this, perm) ==
                            PackageManager.PERMISSION_GRANTED
                }

            if (lacking.isNotEmpty()) {
                // 설명용 다이얼로그를 띄웠다면 그곳에서 launch() 호출,
                // 아니면 바로 요청해도 무방
                nearbyPermLauncher.launch(lacking.toTypedArray())
                return
            }
        }

        // 권한 OK → 실제 토글
        isSearching = !isSearching
        if (isSearching) nearbyMgr.start(userNickname)
        else             nearbyMgr.stop()
    }


    /**
     * OS 버전에 맞춰 필요한 권한 목록을 돌려줍니다.
     */
    private fun requiredNearbyPermissions(): List<String> {
        val perms = mutableListOf<String>()

        /* 위치 권한 */
        perms += Manifest.permission.ACCESS_COARSE_LOCATION
        perms += Manifest.permission.ACCESS_FINE_LOCATION

        /* Wi-Fi 권한 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            perms += Manifest.permission.ACCESS_WIFI_STATE
            perms += Manifest.permission.CHANGE_WIFI_STATE
        }

        /* Bluetooth 권한 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {          // Android 12 +
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_ADVERTISE
            perms += Manifest.permission.BLUETOOTH_CONNECT
        } else {                                                       // Android 11 이하
            perms += Manifest.permission.BLUETOOTH_ADMIN
        }

        return perms
    }


    /* ------------------------- Foreground 재시동 ------------------ */
    override fun onResume() {
        super.onResume()
        if (!ForegroundUtil.isServiceRunning()) ForegroundUtil.startForegroundService(this)
    }

    override fun onDestroy() {
        if (::healthMgr.isInitialized) healthMgr.stopCollecting()
        nearbyMgr.stop(); super.onDestroy()
    }
}

/* =================================================================== */
/*                            MainScreen                               */
/* =================================================================== */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun MainScreen(
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit,
    isFirstUser: Boolean,
    isSearching: Boolean,
    toggleNearby: () -> Unit,
    nearbyMgr: NearbyConnectionManager,
    showFriendBanner: Boolean,
    friendName: String,
    dismissBanner: () -> Unit,
    showPermDialog: Boolean,
    onPermDismiss: () -> Unit
) {
    val navController = rememberNavController()
    val navBack by navController.currentBackStackEntryAsState()
    val currentDest = navBack?.destination
    val currentRoute = when {
        currentDest?.hierarchy?.any { it.route == NavRoutes.HOME } == true -> NavRoutes.HOME
        currentDest?.hierarchy?.any { it.route == NavRoutes.COLLECTION } == true -> NavRoutes.COLLECTION
        currentDest?.hierarchy?.any { it.route == NavRoutes.CALENDAR } == true -> NavRoutes.CALENDAR
        currentDest?.hierarchy?.any { it.route == NavRoutes.MYINFO } == true -> NavRoutes.MYINFO
        currentDest?.hierarchy?.any { it.route == NavRoutes.MYINFOUPDATE } == true -> NavRoutes.MYINFO
        currentDest?.hierarchy?.any { it.route == NavRoutes.EGGMANAGER } == true -> NavRoutes.EGGMANAGER
        else -> NavRoutes.HOME
    }

    /* 동적 배경 */
    var currentBg by remember { mutableIntStateOf(R.drawable.background_sunny_sky) }
    LaunchedEffect(BansoogiStateHolder.state) { currentBg = BansoogiStateHolder.background() }

    /* 빛 애니메이션 */
    val lightPos = remember { Animatable(-1000f) }
    LaunchedEffect(Unit) {
        lightPos.animateTo(
            2000f, animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    /* 친구 목록 Flow */
    val peers by nearbyMgr.peers.collectAsState()

    /* 권한 런처 */
    /* Launcher → 권한 성공 시 기존 로직 재시도 */
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onPermDismiss()                                 // 다이얼로그 닫기
        if (result.values.all { it }) toggleNearby()    // ✅ 재시도
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentBg,
            transitionSpec = {
                slideInHorizontally(
                    tween(1000), { -it }
                ) togetherWith slideOutHorizontally(
                    tween(1000), { it }
                )
            }
        ) { resId ->
            Image(
                painterResource(resId),
                null,
                Modifier.fillMaxSize(),
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
                                    popUpTo(NavRoutes.HOME) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (currentRoute == NavRoutes.HOME) {
                    NearbyFloatingButton(isSearching, toggleNearby)
                }
            }
        ) { padding ->
            val mod = if (currentRoute == NavRoutes.EGGMANAGER)
                Modifier.fillMaxSize() else Modifier.padding(padding)

            /* Nav Graph */
            AppNavGraph(
                navController = navController,
                modifier      = mod,
                healthData    = healthData,
                onModalOpen   = onModalOpen,
                onModalClose  = onModalClose,
                isFirstUser   = isFirstUser
            )

            /* 친구 배너 + 리스트 (Home 화면) */
            if (currentRoute == NavRoutes.HOME) {
                NearbyStatusBanner(isSearching = isSearching, peers = peers,
                    modifier = Modifier.align(Alignment.TopCenter))

                FriendFoundNotification(
                    friendName,
                    showFriendBanner,
                    dismissBanner,
                    Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(1f)                    // z축에서 높은 우선순위
                )
                if (peers.isNotEmpty()) {
                    FriendList(
                        peers, nearbyMgr, Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 48.dp) // ← 추가됨
                    )
                }
            }
        }
    }

    /* 권한 설명 다이얼로그 */
    if (showPermDialog) {
        PermissionDialog(
            onConfirm = { permLauncher.launch(NearbyPermissionManager.requiredPermissions()) },
            onDismiss = onPermDismiss
        )
    }
}

/* =================================================================== */
/*                  Home 관련 보조 로직 (FriendList 등)                */
/* =================================================================== */
//private fun RealmInstant.toLocalDate(): LocalDate =
//    Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())
//        .atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun FriendList(peers: List<BansoogiFriend>, nearbyMgr: NearbyConnectionManager, modifier: Modifier = Modifier) {
    val show = peers.take(3)
    LazyColumn(modifier.widthIn(max = 300.dp)) {
        items(show.size) { idx ->
            val p = show[idx]
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        nearbyMgr.sendStaticWarnTo(p.endpointId, "SITTING_LONG", 60)
                    }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("🥚 ${p.nickname}", fontWeight = FontWeight.Bold)
                    p.distanceRssi?.let {
                        Text("신호세기: ${it}dBm", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (peers.size > 3) item {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "... 외 ${peers.size - 3}명",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/* 권한 다이얼로그 (재사용) */
@Composable
fun PermissionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("근거리 권한 필요") },
        text  = { Text("주변 친구를 찾으려면 블루투스·Wi-Fi 권한이 필요합니다.") },
        confirmButton = { TextButton(onConfirm) { Text("권한 허용") } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } }
    )
}

@Composable
private fun NearbyStatusBanner(
    isSearching: Boolean,
    peers: List<BansoogiFriend>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSearching || peers.isNotEmpty(),
        enter = fadeIn(tween(300)),
        exit  = fadeOut(tween(300)),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Surface(
            color = if (isSearching) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSearching) "탐색 중…" else "탐색 중지됨",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "친구 ${peers.size}명",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}