package com.ddc.bansoogi.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ddc.bansoogi.calendar.ui.CalendarScreen
import com.ddc.bansoogi.collection.ui.CollectionScreen
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.ui.HomeScreen
import com.ddc.bansoogi.myInfo.ui.MyInfoScreen
import com.ddc.bansoogi.myInfo.ui.MyInfoUpdateScreen
import com.samsung.android.sdk.health.data.HealthDataStore

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    healthData: CustomHealthData,
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(healthData)
        }

        composable(NavRoutes.COLLECTION) {
            CollectionScreen()
        }

        composable(NavRoutes.CALENDAR) {
            CalendarScreen()
        }

        composable(NavRoutes.MYINFO) {
            MyInfoScreen(navController)
        }

        composable(NavRoutes.MYINFOUPDATE) {
            MyInfoUpdateScreen(navController)
        }
    }
}