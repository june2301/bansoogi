package com.ddc.bansoogi.landing.ui.screen

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.component.NicknameTextField
import com.ddc.bansoogi.landing.ui.component.NextButton
import com.ddc.bansoogi.landing.ui.component.RoundedContainerBox
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("UnrememberedMutableState")
@Composable
fun ProfileInputScreen(controller: LandingController, onNext: () -> Unit) {

    val context = LocalContext.current
    val nicknameState = remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf(Date()) }
    var birthDateText by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            birthDate = calendar.time
            birthDateText = SimpleDateFormat(
                "yyyy.MM.dd",
                Locale.getDefault()
            ).format(birthDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column {
        RoundedContainerBox {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "프로필 정보",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                NicknameTextField(
                    8,
                    nicknameState,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 생년월일 입력
                OutlinedTextField(
                    value = birthDateText,
                    onValueChange = { }, // 텍스트 직접 수정은 막음
                    readOnly = true, // 원래 없었음
                    placeholder = { Text("생년월일 (YYYY.MM.DD)") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "날짜 선택"
                        )
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "날짜 선택")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() } // 텍스트창 클릭도 허용
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 다음 버튼
        NextButton(
            text = "Next!",
            enabled = nicknameState.value.isNotBlank() && birthDateText.isNotBlank(),
            onClick = {
                if (nicknameState.value.isNotBlank() && birthDateText.isNotBlank()) {
                    //TODO: 직접 변경하지 않도록 변경
                    controller.profileModel.nickname = nicknameState.value
                    controller.profileModel.birthDate = birthDate
                    onNext()
                }
            },
            contentButtonColor = Color(0xFF4CABFD),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
    }
}
