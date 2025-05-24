package com.ddc.bansoogi.landing.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.component.AgreementDialog
import com.ddc.bansoogi.landing.ui.component.AgreementType
import com.ddc.bansoogi.landing.ui.component.CheckboxRow
import com.ddc.bansoogi.landing.ui.component.text.DefaultTitleText
import com.ddc.bansoogi.landing.ui.component.NextButton
import com.ddc.bansoogi.landing.ui.component.RoundedContainerBox
import com.ddc.bansoogi.phoneUsage.PhoneUsagePermissionUtil
import android.app.Activity
import androidx.compose.runtime.rememberCoroutineScope
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.ddc.bansoogi.common.util.health.Permissions
import kotlinx.coroutines.launch

@Composable
fun TermsScreen(controller: LandingController, onNext: () -> Unit) {

    var serviceChecked by remember { mutableStateOf(controller.termsModel.serviceChecked) }
    var privacyChecked by remember { mutableStateOf(controller.termsModel.privacyChecked) }
    var healthChecked by remember { mutableStateOf(controller.termsModel.healthChecked) }

    var currentDialogType by remember { mutableStateOf<AgreementType?>(null) }

    val context = LocalContext.current

    val activity = (context as? Activity)
        ?: error("TermsScreen 은 Activity 컨텍스트에서 호출되어야 합니다.")

    val coroutineScope = rememberCoroutineScope()

    val healthDataStore = remember {
        HealthDataService.getStore(activity)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ContextCompat.startActivity(context, intent, null)
                }
            }
            onNext()
        } else {
            Toast.makeText(
                context,
                "알림 권한이 거부되었습니다. 내 정보에서 수동으로 허용할 수 있습니다.",
                Toast.LENGTH_LONG
            ).show()
            onNext()
        }
    }

    fun requestPermissionThenNext(proceed: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                proceed()
            }
        } else {
            proceed()
        }
    }

    Column {
        RoundedContainerBox {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DefaultTitleText("서비스 이용 약관")
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        NextButton(
            enabled = serviceChecked && privacyChecked,
            onClick = {
                if (!PhoneUsagePermissionUtil.hasUsageStatsPermission(context)) {
                    PhoneUsagePermissionUtil.requestUsageStatsPermission(context)
                    return@NextButton
                }

                coroutineScope.launch {
                    try {
                        healthDataStore.requestPermissions(
                            Permissions.PERMISSIONS,
                            activity
                        )
                    } catch (_: Exception) { /* 무시 */ }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onNext()
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
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