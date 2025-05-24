package com.ddc.bansoogi

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.ddc.bansoogi.common.notification.NotificationDispatcher
import com.ddc.bansoogi.common.notification.NotificationHelper
import com.ddc.bansoogi.common.util.PermissionSyncer
import com.ddc.bansoogi.myInfo.data.local.MyInfoDataSource
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        NotificationHelper.registerChannels(this)
        NotificationDispatcher.bindNotificationEnabled(
            MyInfoModel().notificationEnabledFlow()
        )

        // 알림 권한 <-> DB 동기화
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(PermissionSyncer(this, MyInfoDataSource()))

    }

    override fun onTerminate() {
        super.onTerminate()
    }
}