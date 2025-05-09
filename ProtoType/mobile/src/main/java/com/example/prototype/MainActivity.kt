package com.example.prototype

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_FOREGROUND_SERVICE_DATA_SYNC = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            val permission = "android.permission.FOREGROUND_SERVICE_DATA_SYNC"
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_FOREGROUND_SERVICE_DATA_SYNC,
                )
            } else {
                // 권한이 이미 허용됨, 서비스 시작
                startService(Intent(this, ProtoBleReceiverService::class.java))
            }
        } else {
            // Android 13 이하에서는 기존처럼 서비스 바로 시작
            startService(Intent(this, ProtoBleReceiverService::class.java))
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ActivityScreen(ProtoBleReceiverService.activityLiveData.asFlow(), ProtoBleReceiverService.poseLiveData.asFlow())
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FOREGROUND_SERVICE_DATA_SYNC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 승인됨, 서비스 시작
                startService(Intent(this, ProtoBleReceiverService::class.java))
            } else {
                Toast.makeText(this, "데이터 동기화용 포그라운드 서비스 권한이 필요합니다", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun ActivityScreen(
    activityFlow: Flow<Int>,
    poseFlow: Flow<Int>,
) {
    val activityState by activityFlow.collectAsState(initial = 0)
    val poseState by poseFlow.collectAsState(initial = -1)

    Log.d("MainActivity", "activity=$activityState pose=$poseState")

    val activityText =
        when (activityState) {
            0 -> "정지"
            1 -> "걷는 중"
            2 -> "뛰는 중"
            3 -> "오르는 중"
            else -> "알 수 없음"
        }
    val poseText =
        when (poseState) {
            0 -> "앉아 있음"
            1 -> "누워 있음"
            2 -> "서 있는 중"
            -1 -> "(정적 자세 미수신)"
            else -> "알 수 없음"
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "활동 상태",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = activityText,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
        )

        Text(
            text = "자세: $poseText",
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        )
    }
}
