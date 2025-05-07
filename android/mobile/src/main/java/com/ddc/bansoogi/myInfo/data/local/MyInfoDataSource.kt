package com.ddc.bansoogi.myInfo.data.local

import android.content.Context
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.myInfo.data.entity.User
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.notification.AlarmScheduler
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto

class MyInfoDataSource {
    private val realm = RealmManager.realm

    val notificationEnabledFlow: Flow<Boolean> =
        realm.query<User>().asFlow()
            .map { it.list.firstOrNull()?.notificationEnabled ?: true }

    fun getMyInfo(): Flow<User> =
        realm.query<User>()
            .asFlow()
            .map { it.list.firstOrNull() ?: User() }

    // TODO: 더미데이터 값 설정(나중에 controller 한 줄과 함께 지우면 됩니다)
    // TODO: (+) 프로필 정보 입력할 때 알림 설정하는 로직 그대로 옮겨줘야 함
    suspend fun initialize(appCtx: Context) {
        val hasUser = realm.query<User>().find().isNotEmpty()
        if (hasUser) return

        val dummy = User().apply {
            nickname           = "엄계란"
            birthDate          = "2000.02.16"
            profileBansoogiId  = R.drawable.bansoogi_default_profile
            wakeUpTime         = "07:00"
            sleepTime          = "23:00"
            breakfastTime      = "07:30"
            lunchTime          = "15:12"
            dinnerTime         = "18:30"
            notificationDuration = 30
            notificationEnabled  = false
            bgSoundEnabled       = false
            effectSoundEnabled   = false
        }

        realm.write { copyToRealm(dummy) }

        AlarmScheduler.scheduleAllDailyAlarms(
            appCtx,
            MyInfoDto(
                userId               = dummy.userId.toHexString(),
                nickname             = dummy.nickname,
                birthDate            = dummy.birthDate,
                profileBansoogiId    = dummy.profileBansoogiId,
                wakeUpTime           = dummy.wakeUpTime,
                sleepTime            = dummy.sleepTime,
                breakfastTime        = dummy.breakfastTime,
                lunchTime            = dummy.lunchTime,
                dinnerTime           = dummy.dinnerTime,
                notificationDuration = dummy.notificationDuration,
                notificationEnabled  = dummy.notificationEnabled,
                bgSoundEnabled       = dummy.bgSoundEnabled,
                effectSoundEnabled   = dummy.effectSoundEnabled
            )
        )
    }

    /** 토글 업데이트 */
    suspend fun toggleNotificationEnabled() {
        realm.write {
            query<User>().first().find()?.let { it.notificationEnabled = !it.notificationEnabled }
        }
    }
    suspend fun toggleBgSoundEnabled() {
        realm.write {
            query<User>().first().find()?.let { it.bgSoundEnabled = !it.bgSoundEnabled }
        }
    }
    suspend fun toggleEffectSoundEnabled() {
        realm.write {
            query<User>().first().find()?.let { it.effectSoundEnabled = !it.effectSoundEnabled }
        }
    }

    /** 알림 권한 관련 */
    suspend fun setNotificationEnabled(enabled: Boolean) {
        realm.write {
            query<User>().first().find()?.let { it.notificationEnabled = enabled }
        }
    }

    /** 전체 업데이트 */
    suspend fun updateUser(updated: User) {
        realm.write {
            copyToRealm(updated, updatePolicy = io.realm.kotlin.UpdatePolicy.ALL)
        }
    }

}
