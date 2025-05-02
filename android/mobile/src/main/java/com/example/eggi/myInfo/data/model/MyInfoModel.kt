package com.example.eggi.myInfo.data.model

import com.example.eggi.myInfo.data.entity.User
import com.example.eggi.myInfo.data.local.MyInfoDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId

class MyInfoModel {
    private val dataSource = MyInfoDataSource()

    fun getMyInfo(): Flow<MyInfo> =
        dataSource.getMyInfo().map { entity -> entity.toDomain() }

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

    /** 토글: DB 반영 후 최신값(domain) 리턴 */
    suspend fun toggleAlarm(): MyInfo {
        dataSource.toggleAlarmEnabled()
        val updatedEntity = dataSource.getMyInfo().first()
        return updatedEntity.toDomain()
    }
    suspend fun toggleBgSound(): MyInfo {
        dataSource.toggleBgSoundEnabled()
        val updatedEntity = dataSource.getMyInfo().first()
        return updatedEntity.toDomain()
    }
    suspend fun toggleEffect(): MyInfo {
        dataSource.toggleEffectSoundEnabled()
        val updatedEntity = dataSource.getMyInfo().first()
        return updatedEntity.toDomain()
    }

    /** Entity → Domain 매핑 */
    private fun User.toDomain(): MyInfo = MyInfo(
        userId               = this.userId.toHexString(),
        nickname             = this.nickname,
        birthDate            = this.birthDate,
        profileBansoogiId    = this.profileBansoogiId,
        wakeUpTime           = this.wakeUpTime,
        sleepTime            = this.sleepTime,
        breakfastTime        = this.breakfastTime,
        lunchTime            = this.lunchTime,
        dinnerTime           = this.dinnerTime,
        notificationDuration = this.notificationDuration,
        alarmEnabled         = this.alarmEnabled,
        bgSoundEnabled       = this.bgSoundEnabled,
        effectSoundEnabled   = this.effectSoundEnabled
    )
}
