package com.ddc.bansoogi.myInfo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.ddc.bansoogi.common.navigation.NavRoutes

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ddc.bansoogi.collection.data.model.CollectionModel
import com.ddc.bansoogi.common.util.openAppNotificationSettings


private val GreenChecked = Color(0xFF99CC00)

@Composable
fun MyInfoScreen(navController: NavController) {

    val controller = remember { MyInfoController() }
    val ctx = LocalContext.current

    val myInfo by controller.myInfoFlow()
        .collectAsState(initial = null)

    myInfo?.let { info ->
        MyInfoContent(
            myInfoDto       = info,
            onEdit          = { navController.navigate(NavRoutes.MYINFOUPDATE) },
            onToggleNotification   = { controller.toggleNotification() },
            onToggleBgSound = { controller.toggleBgSound() },
            onToggleEffect  = { controller.toggleEffect() },
            onDeleteMemberData  = { controller.deleteMemberData(ctx) }
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("로딩 중...", fontSize = 16.sp)
    }
}

@Composable
fun MyInfoContent(
    myInfoDto: MyInfoDto,
    onEdit: () -> Unit,
    onToggleNotification: () -> Unit,
    onToggleBgSound: () -> Unit,
    onToggleEffect: () -> Unit,
    onDeleteMemberData: () -> Unit
) {
    val context = LocalContext.current
    val collectionModel = remember { CollectionModel() }
    val imageResId = remember(myInfoDto.profileBansoogiId) {
        collectionModel.getImageResId(context, myInfoDto.profileBansoogiId)
    }
    var showAlert by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "프로필 이미지",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(myInfoDto.nickname, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            myInfoDto.birthDate,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onEdit,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "편집",
                tint = Color(0xFF888888)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("프로필 편집", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(thickness = 2.dp, color = Color(0xFF888888))

        Spacer(modifier = Modifier.height(16.dp))
        SettingRow("기상 희망 시간", myInfoDto.wakeUpTime)
        Spacer(modifier = Modifier.height(4.dp))
        SettingRow("취침 희망 시간", myInfoDto.sleepTime)
        Spacer(modifier = Modifier.height(16.dp))

        Divider(thickness = 2.dp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "식사 희망 시간",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (myInfoDto.breakfastTime.isNotBlank()) {
            SettingRow(
                label             = "아침",
                value             = myInfoDto.breakfastTime,
                labelFontSize     = 20,
                labelColor        = Color(0xFF888888),
                labelStartPadding = 16.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (myInfoDto.lunchTime.isNotBlank()) {
            SettingRow(
                label             = "점심",
                value             = myInfoDto.lunchTime,
                labelFontSize     = 20,
                labelColor        = Color(0xFF888888),
                labelStartPadding = 16.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (myInfoDto.dinnerTime.isNotBlank()) {
            SettingRow(
                label             = "저녁",
                value             = myInfoDto.dinnerTime,
                labelFontSize     = 20,
                labelColor        = Color(0xFF888888),
                labelStartPadding = 16.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(thickness = 2.dp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "상태 지속 시간",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "${myInfoDto.notificationDuration}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = " 분",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(thickness = 2.dp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(8.dp))

        NotificationToggleRow(
            checked = myInfoDto.notificationEnabled,
            onToggle = onToggleNotification
        )
        ToggleRow(
            label = "배경음 설정",
            checked = myInfoDto.bgSoundEnabled,
            onToggle = onToggleBgSound
        )
        ToggleRow(
            label = "효과음 설정",
            checked = myInfoDto.effectSoundEnabled,
            onToggle = onToggleEffect
        )

        Spacer(modifier = Modifier.height(12.dp))
        Divider(thickness = 2.dp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bansoogi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF555555),
                modifier = Modifier.padding(start = 10.dp)
            )
            Text(
                text = "버전 정보 : v1.0.1",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF555555),
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        TextButton(
            onClick = { showAlert = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = "회원 데이터 삭제",
                fontSize = 16.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Bold
            )
        }

    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },

            shape           = RoundedCornerShape(28.dp),
            containerColor  = Color.White,
            tonalElevation  = 6.dp,

            title = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "회원 데이터 삭제",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            },
            text = {
                Text(
                    text = "데이터 삭제를 진행하실 경우,\n" +
                            "Bansoogi 애플리케이션에서\n" +
                            "현재까지 수집·기록된\n" +
                            "모든 데이터가 삭제됩니다.",
                    fontSize  = 18.sp,
                    lineHeight= 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },

            confirmButton = {
                TextButton(onClick = {
                    showAlert = false
                    onDeleteMemberData()
                }) {
                    Text("확인", fontSize = 20.sp, color = Color(0xFF555555))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlert = false }) {
                    Text("취소", fontSize = 20.sp, color = Color(0xFF555555))
                }
            }
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    labelFontSize: Int = 20,
    valueFontSize: Int = 20,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelStartPadding: Dp = 8.dp,
    valueEndPadding: Dp = 8.dp
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = labelFontSize.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier.padding(start = labelStartPadding)
        )
        Text(
            text = value,
            fontSize = valueFontSize.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = valueEndPadding)
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.padding(end = 8.dp),
            colors = SwitchDefaults.colors(
                checkedTrackColor = GreenChecked
            )
        )
    }
}

@Composable
private fun NotificationToggleRow(
    checked: Boolean,
    onToggle: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        ToggleRow(label = "알림 설정", checked = checked, onToggle = onToggle)
        return
    }

    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    val permissionGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                onToggle()
            } else {
                val rationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                )
                if (!rationale) showSettingsDialog = true
                else Toast.makeText(context, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "알림 설정",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Switch(
            checked = permissionGranted && checked,
            onCheckedChange = { wantOn ->
                if (!wantOn) {
                    onToggle(); return@Switch
                }
                if (permissionGranted) {
                    onToggle()
                } else {
                    showSettingsDialog = false
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            modifier = Modifier.padding(end = 8.dp),
            colors = SwitchDefaults.colors(checkedTrackColor = GreenChecked)
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    context.openAppNotificationSettings()
                }) { Text("설정으로 이동") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("취소") }
            },
            title = { Text("알림 설정") },
            text  = { Text("알림 기능을 사용하려면 시스템 설정에서 권한을 켜 주세요.") },
            containerColor = Color(0xFFFFFFFF)

        )
    }

    DisposableEffect(Unit) {
        onDispose { showSettingsDialog = false }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && !checked) onToggle()
    }
}

/* Context → Activity 변환 */
private tailrec fun Context.findActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> error("Permission context must be an Activity")
    }
