package com.ddc.bansoogi.landing.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.component.CheckboxRow
import com.ddc.bansoogi.landing.ui.component.DefaultTitleText
import com.ddc.bansoogi.landing.ui.component.NextButton
import com.ddc.bansoogi.landing.ui.component.RoundedContainerBox

@Composable
fun TermsScreen(controller: LandingController, onNext: () -> Unit) {

    var serviceChecked by remember { mutableStateOf(controller.termsModel.serviceChecked) }
    var privacyChecked by remember { mutableStateOf(controller.termsModel.privacyChecked) }
    var healthChecked by remember { mutableStateOf(controller.termsModel.healthChecked) }

    Column {
        RoundedContainerBox {
            Column(
                modifier = Modifier

                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(), // 부모가 크기를 알아야 정렬 가능
                    contentAlignment = Alignment.Center
                ) {
                    DefaultTitleText("서비스 이용 약관")
                }

                CheckboxRow(
                    checked = serviceChecked,
                    onCheckedChange = {
                        serviceChecked = it
                        controller.updateServiceChecked(it)
                    },
                    label = "[필수] Bansoogi 서비스 이용약관에 동의합니다."
                )
                CheckboxRow(
                    checked = privacyChecked,
                    onCheckedChange = {
                        privacyChecked = it
                        controller.updatePrivacyChecked(it)
                    },
                    label = "[필수] 개인정보 처리방침에 동의합니다."
                )
                CheckboxRow(
                    checked = healthChecked,
                    onCheckedChange = {
                        healthChecked = it
                        controller.updateHealthChecked(it)
                    },
                    label = "[필수] 삼성 헬스 데이터 연동에 동의합니다."
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        NextButton(
            text = "start!",
            enabled = serviceChecked && privacyChecked && healthChecked,
             onClick = {
                if (serviceChecked && privacyChecked && healthChecked) {
                    onNext()
                }
            },
            contentButtonColor = Color(0xFF4CABFD),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
    }
}

