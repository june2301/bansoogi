package com.ddc.bansoogi.common.util

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ddc.bansoogi.myInfo.data.local.MyInfoDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PermissionSyncer(
    private val app: Application,
    private val dataSource: MyInfoDataSource
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        syncNotificationPermission() // 앱이 포그라운드로 올라올 때마다 확인
    }

    private fun syncNotificationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            app, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        CoroutineScope(Dispatchers.IO).launch {
            val current = dataSource.getMyInfo().first().notificationEnabled
            if (granted != current) {
                dataSource.setNotificationEnabled(granted) // ← 정확히 맞춰줌
            }
        }
    }
}
