package com.ddc.bansoogi.myInfo.data.local

import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.myInfo.data.entity.User
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ddc.bansoogi.R

class MyInfoDataSource {
    private val realm = RealmManager.realm

    fun getMyInfo(): Flow<User> =
        realm.query<User>()
            .asFlow()
            .map { it.list.firstOrNull() ?: User() }

    // 더미데이터 값 설정(나중에 controller 한 줄과 함께 지우면 됩니다)
    suspend fun initialize() {
        val hasUser = realm.query<User>().find().isNotEmpty()
        if (!hasUser) {
            realm.write {
                copyToRealm(User().apply {
                    nickname = "엄계란"
                    birthDate = "2000.02.16"
                    profileBansoogiId = R.drawable.bansoogi_default_profile
                    wakeUpTime = "07:00"
                    sleepTime = "23:00"
                    breakfastTime = "07:30"
                    lunchTime = "12:00"
                    dinnerTime = "18:30"
                    notificationDuration = 30
                    alarmEnabled = true
                    bgSoundEnabled = false
                    effectSoundEnabled = false
                })
            }
        }
    }

    /** 토글 업데이트 */
    suspend fun toggleAlarmEnabled() {
        realm.write {
            query<User>().first().find()?.let { it.alarmEnabled = !it.alarmEnabled }
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

    /** 전체 업데이트 */
    suspend fun updateUser(updated: User) {
        realm.write {
            copyToRealm(updated, updatePolicy = io.realm.kotlin.UpdatePolicy.ALL)
        }
    }

}
