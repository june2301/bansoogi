/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.ddc.bansoogi.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.main.ui.MainScreen
import com.ddc.bansoogi.main.ui.MenuScreen
import com.ddc.bansoogi.myinfo.ui.MyInfoScreen
import com.ddc.bansoogi.today.ui.TodayRecordedScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            BansoogiApp()
        }
    }
}

@Composable
fun BansoogiApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.MAIN
    ) {
        composable(NavRoutes.MAIN) {
            MainScreen(navController = navController)
        }
        composable(NavRoutes.MENU) {
            MenuScreen(navController = navController)
        }
        composable(NavRoutes.MYINFO) {
            MyInfoScreen()
        }
        composable(NavRoutes.TODAY) {
            TodayRecordedScreen()
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    BansoogiApp()
}