package com.example.eggi.myInfo.data.model

import com.example.eggi.myInfo.data.local.MyInfoDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MyInfoModel {
    private val dataSource = MyInfoDataSource()

    fun getMyInfo(): Flow<MyInfo> =
        dataSource.getMyInfo().map { entity ->
            MyInfo(
                userId             = entity.userId.toString(),
                name               = entity.name,
                birthDate          = entity.birthDate,
                profileBansoogiId  = entity.profileBansoogiId,
                wakeUpTime         = entity.wakeUpTime,
                sleepTime          = entity.sleepTime,
                breakfastTime      = entity.breakfastTime,
                lunchTime          = entity.lunchTime,
                dinnerTime         = entity.dinnerTime,
                notificationDuration    = entity.notificationDuration,
                alarmEnabled       = entity.alarmEnabled,
                bgSoundEnabled     = entity.bgSoundEnabled,
                effectSoundEnabled = entity.effectSoundEnabled
            )
        }
}
