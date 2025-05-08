package com.ddc.bansoogi.landing.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.component.DefaultTitleText
import com.ddc.bansoogi.landing.ui.component.NicknameTextField
import com.ddc.bansoogi.landing.ui.component.NextButton
import com.ddc.bansoogi.landing.ui.component.RoundedContainerBox

@SuppressLint("UnrememberedMutableState")
@Composable
fun ProfileInputScreen(controller: LandingController, onNext: () -> Unit) {

    val nicknameState = remember { mutableStateOf(controller.profileModel.nickname) }

    Column {

        RoundedContainerBox {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                DefaultTitleText("프로필 정보")

                Spacer(modifier = Modifier.height(24.dp))

                NicknameTextField(
                    8,
                    nicknameState,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        NextButton(
            text = "Next!",
            enabled = nicknameState.value.count() in 3..8,
            onClick = {
                if (nicknameState.value.count() in 3..8) {
                    controller.profileModel.nickname = nicknameState.value
                    onNext()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
    }
}
