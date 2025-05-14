package com.ddc.bansoogi.main.ui.util

import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import io.realm.kotlin.types.RealmInstant
import java.time.LocalTime
import java.time.ZoneId

const val MAX_INTERACTION_COUNT = 20

object InteractionUtil {
    fun isInteractionConditionMet(todayRecord: TodayRecordDto): Boolean {
        // 1. 현재 점수 확인
        if (todayRecord.energyPoint >= 100) return false

        // 2. 상호작용 최대 횟수 인지
        if (todayRecord.interactionCnt >= MAX_INTERACTION_COUNT) return false

        return true
    }

    fun isInSleepRange(myInfo: MyInfoDto?): Boolean {
        val koreaZoneId = ZoneId.of("Asia/Seoul")
        val nowTime = LocalTime.now(koreaZoneId)

        val sleepTime = LocalTime.parse(myInfo?.sleepTime)
        val wakeUpTime = LocalTime.parse(myInfo?.wakeUpTime)

        if (sleepTime.isAfter(wakeUpTime)) {
            // 날짜가 바뀌는 경우 (예: 취침 23:00, 기상 07:00)
            return nowTime.isAfter(sleepTime) || nowTime.isBefore(wakeUpTime)
        } else {
            // 같은 날 내에서의 경우 (예: 낮잠 13:00, 기상 15:00)
            return nowTime.isAfter(sleepTime) && nowTime.isBefore(wakeUpTime)
        }
    }

    // 쿨타임(1시간) 내에 있는지 확인하는 함수
    fun isInInteractionCooldown(interactionTime: RealmInstant?): Boolean {
        // interactionTime이 null이면 쿨타임 없음 (처음 상호작용)
        if (interactionTime == null) return false

        val nowRealmInstant = RealmInstant.now()

        // 현재 시간에서 1시간(3600초) 이전의 시간 계산
        val cooldownStartTime = RealmInstant.from(
        nowRealmInstant.epochSeconds - 3600,
            nowRealmInstant.nanosecondsOfSecond
        )

        return interactionTime >= cooldownStartTime
    }

    // 남은 쿨타임 확인
    fun getRemainingCooldownMillis(interactionTime: RealmInstant?): Long {
        if (interactionTime == null) return 0

    val cooldownSeconds = 60 * 60L
        val now = RealmInstant.now()
        val cooldownEnd = RealmInstant.from(
            interactionTime.epochSeconds + cooldownSeconds,
            interactionTime.nanosecondsOfSecond
        )

        val remainingSeconds = cooldownEnd.epochSeconds - now.epochSeconds
        val remainingNanos = cooldownEnd.nanosecondsOfSecond - now.nanosecondsOfSecond

        return (remainingSeconds * 1000) + (remainingNanos / 1_000_000)
            .coerceAtLeast(0)
    }
}
