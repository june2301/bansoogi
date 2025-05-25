package com.ddc.bansoogi.common.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.ddc.bansoogi.calendar.ui.CalendarScreen
import com.ddc.bansoogi.collection.ui.CollectionScreen
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.HomeScreen
import com.ddc.bansoogi.main.ui.manage.CharacterGetScreen
import com.ddc.bansoogi.main.ui.manage.EggManagerScreen
import com.ddc.bansoogi.main.view.TodayRecordView
import com.ddc.bansoogi.myInfo.ui.MyInfoScreen
import com.ddc.bansoogi.myInfo.ui.MyInfoUpdateScreen

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    healthData: CustomHealthData,
    onModalOpen: () -> Unit,
    onModalClose: () -> Unit,
    isFirstUser: Boolean,
    showFriendBanner: Boolean = false,
    friendName: String = "",
    onDismissFriendBanner: () -> Unit = {}
) {
    val context = LocalContext.current

    val todayRecordController = remember {
        TodayRecordController(
            view = object : TodayRecordView {
                override fun displayTodayRecord(todayRecordDto: TodayRecordDto) { }
                override fun showEmptyState() { }
            },
            context = context
        )
    }

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
                showFriendBanner = showFriendBanner,
                friendName = friendName,
                onDismissFriendBanner = onDismissFriendBanner
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

        composable("character_get/{walk}/{stairs}/{sleep}/{exercise}") {backStackEntry ->
            val walk = backStackEntry.arguments?.getString("walk")?.toIntOrNull() ?: 0
            val stairs = backStackEntry.arguments?.getString("stairs")?.toIntOrNull() ?: 0
            val sleep = backStackEntry.arguments?.getString("sleep")?.toIntOrNull() ?: 0
            val exercise = backStackEntry.arguments?.getString("exercise")?.toIntOrNull() ?: 0

            CharacterGetScreen(navController, walk, stairs, sleep, exercise)
        }

        composable(NavRoutes.EGGMANAGER) {
            EggManagerScreen(
                onRenewRecord = { todayRecordController.renewTodayRecord() },
                onDismiss     = { navController.popBackStack() },
                modifier      = Modifier.fillMaxSize()
            )
        }
    }
}