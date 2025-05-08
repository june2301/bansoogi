package com.ddc.bansoogi.landing.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ddc.bansoogi.R
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.screen.BirthInputScreen
import com.ddc.bansoogi.landing.ui.screen.LandingStartScreen
import com.ddc.bansoogi.landing.ui.screen.NicknameInputScreen
import com.ddc.bansoogi.landing.ui.screen.TermsScreen
import com.ddc.bansoogi.landing.ui.screen.TimeSettingScreen
import com.ddc.bansoogi.main.ui.MainActivity

class LandingActivity : ComponentActivity(), LandingView {

    private lateinit var controller: LandingController
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        LandingController.initRealm(applicationContext)

        controller = LandingController(this)

        setContent {
            navController = rememberNavController()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFB3E5FC)) // 원하는 배경색
            ) {
                Image(
                    painter = painterResource(id = R.drawable.background_sunny_sky),
                    contentDescription = "배경 하늘",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            NavHost(navController, startDestination = "start") {
                composable("start") {
                    LandingStartScreen {
                        showTermsScreen()
                    }
                }

                composable("terms") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TermsScreen(controller) {
                            showNicknameInputScreen()
                        }
                    }
                }

                composable("nickname") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NicknameInputScreen(controller) {
                            showBirthInputScreen()
                        }
                    }
                }

                composable("birth") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BirthInputScreen (controller) {
                            showTimeScreen()
                        }
                    }
                }

                composable("time") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimeSettingScreen {
                            moveToMainActivity()
                        }
                    }

                }
            }
        }
    }

    override fun showStartScreen() {
        navController.navigate("start")
    }

    override fun showTermsScreen() {
        navController.navigate("terms")
    }

    override fun showNicknameInputScreen() {
        navController.navigate("nickname")
    }

    override fun showBirthInputScreen() {
        navController.navigate("birth")
    }

    override fun showTimeScreen() {
        navController.navigate("time")
    }

    override fun moveToMainActivity() {
//        val intent = Intent(this, MainActivity::class.java)
//        startActivity(intent)
//        finish()

        navController.popBackStack("start", inclusive = false)
        //여기서 데이터 처리해주면 됨.
        val intent = Intent(this@LandingActivity, MainActivity::class.java)
        // TODO: 데이터 한번에 처리
// 모든 데이터 수집 후 MainActivity로 전달
//                        val intent = Intent(this@LandingActivity, MainActivity::class.java).apply {
//                            putExtra("userName", userName)
//                            putExtra("wakeUpTime", wakeUpTime)
//                            putExtra("serviceChecked", serviceChecked)
//                        }
        startActivity(intent)
        finish()
    }

    override fun showValidationError(errorString: String) {
//        Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show()
    }
}
