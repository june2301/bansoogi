package com.ddc.bansoogi.main.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ddc.bansoogi.common.navigation.AppNavGraph
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.CommonNavigationBar

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var permissionLauncher: ActivityResultLauncher<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "권한이 승인되었습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            MainScreen()
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