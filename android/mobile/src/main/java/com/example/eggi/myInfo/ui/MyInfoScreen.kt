package com.example.eggi.myInfo.ui

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.eggi.myInfo.controller.MyInfoController
import com.example.eggi.myInfo.data.model.MyInfo
import com.example.eggi.myInfo.view.MyInfoView
import com.example.eggi.R
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

private val GreenChecked = Color(0xFF99CC00)

@Composable
fun MyInfoScreen() {
    val infoState = remember { mutableStateOf<MyInfo?>(null) }
    val view = remember {
        object : MyInfoView {
            override fun displayMyInfo(myInfo: MyInfo) {
                infoState.value = myInfo
            }
        }
    }
    val controller = remember { MyInfoController(view) }
    LaunchedEffect(Unit) {
        controller.initialize()
    }

    infoState.value?.let { myInfo ->
        MyInfoContent(myInfo)
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("로딩 중...", fontSize = 16.sp)
    }
}

@Composable
fun MyInfoContent(myInfo: MyInfo) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }
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
            painter = rememberAsyncImagePainter(
                model = myInfo.profileBansoogiId,
                imageLoader = imageLoader
            ),
            contentDescription = "프로필 이미지",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(myInfo.nickname, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            myInfo.birthDate,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { /* 추가 예정 */ },
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
        SettingRow("기상 희망 시간", myInfo.wakeUpTime)
        Spacer(modifier = Modifier.height(4.dp))
        SettingRow("취침 희망 시간", myInfo.sleepTime)

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
        SettingRow("아침", myInfo.breakfastTime, labelFontSize = 20, labelColor = Color(0xFF888888), labelStartPadding = 16.dp)
        Spacer(modifier = Modifier.height(4.dp))
        SettingRow("점심", myInfo.lunchTime, labelFontSize = 20, labelColor = Color(0xFF888888), labelStartPadding = 16.dp)
        Spacer(modifier = Modifier.height(4.dp))
        SettingRow("저녁", myInfo.dinnerTime, labelFontSize = 20, labelColor = Color(0xFF888888), labelStartPadding = 16.dp)

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
                    text = "${myInfo.notificationDuration}",
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

        ToggleRow("알림 설정", myInfo.alarmEnabled)
        ToggleRow("배경음 설정", myInfo.bgSoundEnabled)
        ToggleRow("효과음 설정", myInfo.effectSoundEnabled)
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
    checked: Boolean
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
            onCheckedChange = { /* 변경 처리 */ },
            modifier = Modifier.padding(end = 8.dp),
            colors = SwitchDefaults.colors(
                checkedTrackColor = GreenChecked
            )
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFF,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun MyInfoContentPreview() {
    MaterialTheme {
        MyInfoContent(
            myInfo = MyInfo(
                userId               = "0123456789abcdef",
                nickname                 = "엄계란",
                birthDate            = "2000.02.16",
                profileBansoogiId    = R.drawable.bansoogi_default_profile,
                wakeUpTime           = "07:00",
                sleepTime            = "23:00",
                breakfastTime        = "07:30",
                lunchTime            = "12:00",
                dinnerTime           = "18:30",
                notificationDuration = 30,
                alarmEnabled         = true,
                bgSoundEnabled       = false,
                effectSoundEnabled   = false
            )
        )
    }
}