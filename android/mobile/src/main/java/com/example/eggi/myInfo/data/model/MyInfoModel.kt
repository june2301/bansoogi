package com.example.eggi.myInfo.data.model

import com.example.eggi.myInfo.data.entity.User
import com.example.eggi.myInfo.data.local.MyInfoDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId

class MyInfoModel {
    private val dataSource = MyInfoDataSource()

    fun getMyInfo(): Flow<MyInfo> =
        dataSource.getMyInfo().map { entity ->
            MyInfo(
                userId             = entity.userId.toString(),
                nickname           = entity.nickname,
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

    suspend fun updateMyInfo(input: MyInfo): MyInfo {
        val entity = User().apply {
            userId             = ObjectId(input.userId)
            nickname           = input.nickname
            birthDate          = input.birthDate
            profileBansoogiId  = input.profileBansoogiId
            wakeUpTime         = input.wakeUpTime
            sleepTime          = input.sleepTime
            breakfastTime      = input.breakfastTime
            lunchTime          = input.lunchTime
            dinnerTime         = input.dinnerTime
            notificationDuration = input.notificationDuration
            alarmEnabled         = input.alarmEnabled
            bgSoundEnabled       = input.bgSoundEnabled
            effectSoundEnabled   = input.effectSoundEnabled
        }

        dataSource.updateUser(entity)

        return input
    }
}
