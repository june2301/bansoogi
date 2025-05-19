package com.ddc.bansoogi.phoneUsage

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ddc.bansoogi.common.foreground.NotificationDurationStateHolder
import java.time.LocalDate
import java.time.ZoneId

object PhoneUsageAnalyzer {

    const val BANSOOGI_PACKAGE = "com.ddc.bansoogi"

    object UsageStateHolder {
        var usageStartTime: Long? = null // 핸드폰 사용이 시작된 시각 (밀리초)
        var lastTrackedPackageName: String = "" // 가장 마지막에 사용한 패키지 명
        var lastTrackEventType: Int? = null // 가장 마지막에 발생한 이벤트 타입
    }

    // 현재 핸드폰 사용 여부
    fun isPhoneInUseNow(context: Context, windowMs: Long = 5000): Boolean {
        var now = System.currentTimeMillis()
        var events = getUsageEventsBetween(context, now - windowMs, now) // 5초 안의 이벤트 호출

        var currentEvent = UsageEvents.Event() // 확인할 이벤트

        var lastEventType: Int? = null // 마지막 이벤트 타입
        var lastPackage = "" // 마지막 이벤트 패키지

        while(events.hasNextEvent()) {
            events.getNextEvent(currentEvent)

            // ACTIVITY_RESUMED : 사용자가 Activity를 실제로 보고 있는 상태일 때 발생
            // ACTIVITY_PAUSED : 사용자가 Activity를 더 이상 보고 있지 않게 될 때 발생
//            if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED
//                || currentEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED
//            ) {
//                lastEventType = currentEvent.eventType
//                lastPackage = currentEvent.packageName
//            }

            if (
                currentEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                currentEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                currentEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                currentEvent.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            ) {
                lastEventType = currentEvent.eventType
                lastPackage = currentEvent.packageName
            }
        }

        // 이벤트 존재
        if (lastEventType != null) {
            UsageStateHolder.lastTrackedPackageName = lastPackage
            UsageStateHolder.lastTrackEventType = lastEventType

            // 반숙이 앱 사용 중이면 false 처리
            if (lastPackage == BANSOOGI_PACKAGE){
                return false
            }

            // 최근 이벤트 ACTIVITY_RESUMED, 이벤트 발생 시점이 5초 이내 -> 새로운 앱 시작 추정
            if (lastEventType == UsageEvents.Event.ACTIVITY_RESUMED
                || currentEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                return true
            }
            // 최근 이벤트 ACTIVITY_PAUSED, 이벤트 발생 시점이 5초 이내 -> 앱을 종료 추정
            else if (lastEventType == UsageEvents.Event.ACTIVITY_PAUSED
                || currentEvent.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                return false
            }
        }

        // 이벤트 존재 X 경우

        // 반숙이 앱 사용 중이면 false
        if (UsageStateHolder.lastTrackedPackageName == BANSOOGI_PACKAGE) {
            return false
        }

        // 가장 마지막 발생 이벤트 확인
        return UsageStateHolder.lastTrackEventType == UsageEvents.Event.ACTIVITY_RESUMED
                || currentEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
    }

    // 해당 시간 사이의 이벤트
    fun getUsageEventsBetween(context: Context, start: Long, end:Long): UsageEvents {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        return usm.queryEvents(start, end) // 해당 범위 내의 앱 사용 이벤트 조회
    }

    // 핸드폰 기준 이상 사용 감지
    fun isPhoneUsedOverThreshold(min: Int): Boolean {
        val start = UsageStateHolder.usageStartTime ?: return false
        val now = System.currentTimeMillis()

        return now - start >= min * 60 * 1000
    }

    // 폰 사용 시간 호출
    fun getPhoneUsageMinutes(): Int {
        val start = UsageStateHolder.usageStartTime

        if (start == null) {
            return NotificationDurationStateHolder.notificationDuration
        } else {
            val now = System.currentTimeMillis()
            val ms = now - start

            return (ms / 60_000).toInt()
        }
    }

    // 현재 폰 추적 중인가 -> 폰 추적 시작 시간이 null
    fun isTrackingUsage(): Boolean {
        return UsageStateHolder.usageStartTime != null
    }

    // 추적 시작 시각 설정
    fun startUsageTracking() {
        UsageStateHolder.usageStartTime = System.currentTimeMillis()
    }

    // 추적 시작 시각 null 처리
    fun resetUsageStartTime() {
        UsageStateHolder.usageStartTime = null
    }

    // 반숙이를 제외한 앱의 사용량
    fun getTodayUsageTime(context: Context): Long {
        val total = getTodayTotalUsageTime(context)
        val myApp = getTodayUsageTimeByPackageName(context, "com.ddc.bansoogi")

        return millisToMinutes(total - myApp)
    }

    // 이름을 통해 앱 사용량 호출
    private fun getTodayUsageTimeByPackageName(context: Context, packageName: String): Long{
        val statsMap = getTodayUsageTimeMap(context)

        return statsMap[packageName]?.totalTimeInForeground ?: 0L
    }

    // 모든 앱에 대한 사용량 호출
    private fun getTodayTotalUsageTime(context: Context): Long {
        // 앱별 사용량 호출
        val statsMap = getTodayUsageTimeMap(context)

        // 포그라운드 시간의 합
        return statsMap.values.sumOf { it.totalTimeInForeground }
    }

    private fun getTodayUsageTimeMap(context: Context): Map<String, UsageStats> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // 현재 시작 (밀리초)
        val now = System.currentTimeMillis()

        // 오늘 자정부터 현재 시각까지 밀리초로 변환
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()) // 시스템 시간대 기준 자정 시간
            .toInstant()
            .toEpochMilli() // 밀리초 변환

        // 자정부터 현재까지 앱별 사용량 호출
        return usm.queryAndAggregateUsageStats(startOfDay, now)
    }

    private fun millisToMinutes(ms: Long): Long {
        return  ms / 1000 / 60
    }
}