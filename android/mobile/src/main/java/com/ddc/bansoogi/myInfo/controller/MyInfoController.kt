package com.ddc.bansoogi.myInfo.controller

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.common.notification.AlarmScheduler
import com.ddc.bansoogi.main.ui.SplashActivity
import com.ddc.bansoogi.myInfo.data.local.MyInfoDataSource
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess
import androidx.core.content.edit

class MyInfoController {
    private val model = MyInfoModel()
    private val scope = CoroutineScope(Dispatchers.IO)

    // UI가 구독할 Flow
    fun myInfoFlow(): Flow<MyInfoDto> = model.getMyInfo()

    // 첫번째 사용자 여부
    fun isFirstUser(context: Context): Boolean {
        return model.isFirstUser(context)
    }

    fun markAsFirstUser(context: Context) {
        model.markAsFirstUser(context)
    }

    fun markNotFirstUser(context: Context) {
        model.markNotFirstUser(context)
    }

    // 토글 메서드들 — DB만 갱신하면 Flow가 자동 emit
    fun toggleNotification()   { scope.launch { model.toggleNotification() } }
    fun toggleBgSound() { scope.launch { model.toggleBgSound() } }
    fun toggleEffect()  { scope.launch { model.toggleEffect()  } }

    fun deleteMemberData(appCtx: Context) {
        scope.launch {

            val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancelAll()
            AlarmScheduler.cancelAllDailyAlarms(appCtx)

            RealmManager.clearAll()

            appCtx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit(commit = true) { clear() }

            withContext(Dispatchers.Main) {
                if (appCtx is Activity) appCtx.finishAffinity()

                val intent = Intent(appCtx, SplashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                appCtx.startActivity(intent)
            }
        }
    }

}
