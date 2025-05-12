package com.ddc.bansoogi.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.ddc.bansoogi.main.ui.MainScreen
import com.ddc.bansoogi.main.ui.MenuScreen
import com.ddc.bansoogi.myinfo.ui.MyInfoScreen
import com.ddc.bansoogi.today.ui.TodayRecordedScreen

@Composable
fun WearNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.MAIN,
        modifier = modifier
    ) {

        composable(
            route = NavRoutes.MAIN,
            deepLinks = listOf(
                navDeepLink { uriPattern = "bansoogi://watch/meal?type={type}" },
                navDeepLink { uriPattern = "bansoogi://watch/wake" },
                navDeepLink { uriPattern = "bansoogi://watch/sleep" }
            )
        ) { backStackEntry ->
            // query parameter 가 있으면 꺼내서 MainScreen 에 전달 가능
            MainScreen(navController)
        }

        composable(NavRoutes.MAIN)   { MainScreen(navController) }
        composable(NavRoutes.MENU)   { MenuScreen(navController) }
        composable(NavRoutes.MYINFO) { MyInfoScreen() }
        composable(NavRoutes.TODAY)  { TodayRecordedScreen() }
    }
}
