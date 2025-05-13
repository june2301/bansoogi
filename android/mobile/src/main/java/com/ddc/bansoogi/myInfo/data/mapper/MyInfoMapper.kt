package com.ddc.bansoogi.myInfo.data.mapper

import com.ddc.bansoogi.myInfo.data.entity.User
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto

object MyInfoMapper {
    fun User.toDomain(): MyInfoDto = MyInfoDto(
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
