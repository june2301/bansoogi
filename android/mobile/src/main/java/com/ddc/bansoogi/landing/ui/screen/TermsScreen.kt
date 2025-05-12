package com.ddc.bansoogi.landing.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.component.AgreementDialog
import com.ddc.bansoogi.landing.ui.component.AgreementType
import com.ddc.bansoogi.landing.ui.component.CheckboxRow
import com.ddc.bansoogi.landing.ui.component.text.DefaultTitleText
import com.ddc.bansoogi.landing.ui.component.NextButton
import com.ddc.bansoogi.landing.ui.component.RoundedContainerBox

@Composable
fun TermsScreen(controller: LandingController, onNext: () -> Unit) {

    var serviceChecked by remember { mutableStateOf(controller.termsModel.serviceChecked) }
    var privacyChecked by remember { mutableStateOf(controller.termsModel.privacyChecked) }
    var healthChecked by remember { mutableStateOf(controller.termsModel.healthChecked) }

    var currentDialogType by remember { mutableStateOf<AgreementType?>(null) }

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
                    agreementType = AgreementType.SERVICE,
                    onLabelClick = { currentDialogType = it }
                )

                CheckboxRow(
                    checked = privacyChecked,
                    onCheckedChange = {
                        privacyChecked = it
                        controller.updatePrivacyChecked(it)
                    },
                    agreementType = AgreementType.PRIVACY,
                    onLabelClick = { currentDialogType = it }
                )

                // TODO: Health 사용시에 추가
//                CheckboxRow(
//                    checked = healthChecked,
//                    onCheckedChange = {
//                        healthChecked = it
//                        controller.updateHealthChecked(it)
//                    },
//                    agreementType = AgreementType.HEALTH,
//                    onLabelClick = { currentDialogType = it }
//                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        NextButton(
            // TODO: Health Data 활용 시 추가 예정
//            enabled = serviceChecked && privacyChecked && healthChecked,
//            onClick = {
//                if (serviceChecked && privacyChecked && healthChecked) {
//                    onNext()
//                }
//            },
            enabled = serviceChecked && privacyChecked,
            onClick = {
                if (serviceChecked && privacyChecked) {
                    onNext()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
    }

    currentDialogType?.let { type ->
        AgreementDialog(
            agreementType = type,
            onDismiss = { currentDialogType = null },
            onAgree = {
                when (type) {
                    AgreementType.SERVICE -> {
                        serviceChecked = true
                        controller.updateServiceChecked(true)
                    }
                    AgreementType.PRIVACY -> {
                        privacyChecked = true
                        controller.updatePrivacyChecked(true)
                    }
                    AgreementType.HEALTH -> {
                        healthChecked = true
                        controller.updateHealthChecked(true)
                    }
                }
                currentDialogType = null
            }
        )
    }
}
