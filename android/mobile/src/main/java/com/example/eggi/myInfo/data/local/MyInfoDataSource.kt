package com.example.eggi.myInfo.data.local

import com.example.eggi.common.data.local.RealmManager
import com.example.eggi.myInfo.data.entity.User
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.eggi.R

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

}
