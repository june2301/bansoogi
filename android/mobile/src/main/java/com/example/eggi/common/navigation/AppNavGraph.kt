package com.example.eggi.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.eggi.calendar.ui.CalendarScreen
import com.example.eggi.collection.ui.CollectionScreen
import com.example.eggi.main.ui.HomeScreen
import com.example.eggi.myInfo.ui.MyInfoScreen

@Composable
fun AppNavGraph(navController: NavHostController,
                modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen()
        }

        composable(NavRoutes.COLLECTION) {
            CollectionScreen()
        }

        composable(NavRoutes.CALENDAR) {
            CalendarScreen()
        }

        composable(NavRoutes.MYINFO) {
            MyInfoScreen()
        }
    }
}