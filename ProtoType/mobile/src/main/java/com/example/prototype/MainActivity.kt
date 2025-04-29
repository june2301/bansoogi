package com.example.prototype

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    color = MaterialTheme.colorScheme.background
                ) {
                    ActivityDetectionScreen(ProtoBleReceiverService.stateLiveData.asFlow())
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
fun ActivityDetectionScreen(stateFlow: Flow<ActivityState>) {
    val state by stateFlow.collectAsState(initial = ActivityState.TRANSIENT)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "현재 상태",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text =
                when (state) {
                    ActivityState.SITTING -> "앉음"
                    ActivityState.STANDING -> "서있음"
                    ActivityState.LYING -> "누움"
                    ActivityState.WALKING -> "걷기"
                    ActivityState.RUNNING -> "달리기"
                    ActivityState.STAIR_UP -> "+1 층"
                    ActivityState.EXERCISE -> "운동"
                    ActivityState.DYNAMIC_GENERIC -> "동적"
                    ActivityState.TRANSIENT -> "전환 중"
            },
                fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        )
    }
}