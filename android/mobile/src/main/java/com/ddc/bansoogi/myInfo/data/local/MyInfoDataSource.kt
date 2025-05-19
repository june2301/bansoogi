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
import androidx.core.content.edit

class MyInfoDataSource {
    private val realm = RealmManager.realm
    private val prefsName = "bansoogi_prefs"

    val notificationEnabledFlow: Flow<Boolean> =
        realm.query<User>().asFlow()
            .map { it.list.firstOrNull()?.notificationEnabled ?: true }

    fun getMyInfo(): Flow<User> =
        realm.query<User>()
            .asFlow()
            .map { it.list.firstOrNull() ?: User() }

    fun getMyInfoSync(): User? =
        realm.query<User>().first().find()

    suspend fun updateProfileBansoogiId(bansoogiId: Int) {
        realm.write {
            query<User>().first().find()?.let {
                it.profileBansoogiId = bansoogiId
            }
        }
    }

    /** 첫번째 사용자 여부 **/
    fun setFirstUserTrue(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit() { putBoolean("isFirstUser", true) }
    }

    fun setFirstUserFalse(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit() { putBoolean("isFirstUser", false) }
    }

    fun isFirstUser(context: Context): Boolean {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean("isFirstUser", false)
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
