package com.ddc.bansoogi.main.ui.manage

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.data.model.DetailReportDto
import com.ddc.bansoogi.calendar.ui.RecordContent
import com.ddc.bansoogi.common.notification.NotificationDispatcher
import com.ddc.bansoogi.common.notification.NotificationFactory
import com.ddc.bansoogi.main.ui.handle.handleInteraction
import kotlinx.coroutines.launch

@Composable
fun BTN () {
    val buttonShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier.width(180.dp)
    ) {
        Button(
            onClick = { },
            modifier = Modifier
                .padding(vertical = 10.dp)
                .height(60.dp)
                .fillMaxWidth()
                .border(
                    width = 4.dp,
                    color = Color.DarkGray,
                    shape = buttonShape
                ),
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF2E616A)
            )
        ) {
            // 텍스트 중앙 배치
            Box(modifier = Modifier
                .border(1.dp, Color.Magenta)
                .fillMaxSize()
            ) {
                Text(
                    text = "상호작용",
                    color = Color(0xFF2E616A),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, Color.Blue)
                        .align(Alignment.Center)
                )

                // 시계 이모티콘 오른쪽 상단 배치
                Image(
                    painter = painterResource(id = R.drawable.schedule), // 이미지 리소스
                    contentDescription = "Clock Icon",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
//                        .padding(top = 4.dp, end = 4.dp)
                        .border(1.dp, Color.Red)
                        .height(16.dp)
                )
            }

        }
    }


}

@Preview(showBackground = true)
@Composable
fun BBBPreview() {
    BTN()
}