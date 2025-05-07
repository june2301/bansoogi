package com.ddc.bansoogi.myInfo.data.model

import com.ddc.bansoogi.myInfo.data.entity.User
import com.ddc.bansoogi.myInfo.data.local.MyInfoDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId

class MyInfoModel {
    private val dataSource = MyInfoDataSource()

    fun notificationEnabledFlow(): Flow<Boolean> = dataSource.notificationEnabledFlow

    fun getMyInfo(): Flow<MyInfoDto> =
        dataSource.getMyInfo().map { entity -> entity.toDomain() }

    suspend fun updateMyInfo(input: MyInfoDto): MyInfoDto {
        val entity = User().apply {
            userId               = ObjectId(input.userId)
            nickname             = input.nickname
            birthDate            = input.birthDate
            profileBansoogiId    = input.profileBansoogiId
            wakeUpTime           = input.wakeUpTime
            sleepTime            = input.sleepTime
            breakfastTime        = input.breakfastTime
            lunchTime            = input.lunchTime
            dinnerTime           = input.dinnerTime
            notificationDuration = input.notificationDuration
            notificationEnabled  = input.notificationEnabled
            bgSoundEnabled       = input.bgSoundEnabled
            effectSoundEnabled   = input.effectSoundEnabled
        }
        dataSource.updateUser(entity)
        return input
    }

    /** 토글: DB 반영 후 최신값(domain) 리턴 */
    suspend fun toggleNotification(): MyInfoDto {
        dataSource.toggleNotificationEnabled()
        val updatedEntity = dataSource.getMyInfo().first()
        return updatedEntity.toDomain()
    }
    suspend fun toggleBgSound(): MyInfoDto {
        dataSource.toggleBgSoundEnabled()
        val updatedEntity = dataSource.getMyInfo().first()
        return updatedEntity.toDomain()
    }
    suspend fun toggleEffect(): MyInfoDto {
        dataSource.toggleEffectSoundEnabled()
        val updatedEntity = dataSource.getMyInfo().first()
        return updatedEntity.toDomain()
    }

    /** Entity → Domain 매핑 */
    private fun User.toDomain(): MyInfoDto = MyInfoDto(
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
        notificationEnabled  = this.notificationEnabled,
        bgSoundEnabled       = this.bgSoundEnabled,
        effectSoundEnabled   = this.effectSoundEnabled
    )
}
