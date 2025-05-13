package com.ddc.bansoogi.common.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.ddc.bansoogi.calendar.ui.CalendarScreen
import com.ddc.bansoogi.collection.ui.CollectionScreen
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.ui.HomeScreen
import com.ddc.bansoogi.main.ui.egg.CharacterGetScreen
import com.ddc.bansoogi.myInfo.ui.MyInfoScreen
import com.ddc.bansoogi.myInfo.ui.MyInfoUpdateScreen
import com.samsung.android.sdk.health.data.HealthDataStore

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier
    ) {

        composable(
            route = NavRoutes.HOME,
            deepLinks = listOf(
                navDeepLink { uriPattern = "bansoogi://home" }
            )
        ) {
            HomeScreen(
                healthData,
                onModalOpen = onModalOpen,
                onModalClose = onModalClose,
                navController = navController,
            )
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

        composable("character_get") {
            CharacterGetScreen(navController)
        }
    }
}