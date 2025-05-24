package com.ddc.bansoogi.phoneUsage

import android.app.usage.UsageEvents
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PhoneUsageFinishTracker {
    
    var notificationTime: Long = 0
    var hasAddEnergy: Boolean = false

    fun startMonitoring(context: Context, scope: CoroutineScope, onReward: suspend () -> Unit) {
        notificationTime = System.currentTimeMillis()
        hasAddEnergy = false

        scope.launch {
            repeat(60) { // 60번
                delay(5000) // 5초

                if (isPhoneUseFinished(context)) {
                    if (!hasAddEnergy) { // 이미 에너지 점수 부여 했는지 확인
                        hasAddEnergy = true

                        onReward() // 에너지 추가
                    }
                    return@launch
                }
            }
        }
    }

    fun isPhoneUseFinished(context: Context): Boolean {
        // 알림 발생 부터 현재 이벤트 확인
        val now = System.currentTimeMillis()
        val events = PhoneUsageAnalyzer.getUsageEventsBetween(context, notificationTime, now)

        val currentEvent = UsageEvents.Event()

        while(events.hasNextEvent()) {
            events.getNextEvent(currentEvent)

            // 1. 반숙이 앱에 접속 -> 폰 off 처리
            if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                && currentEvent.packageName == PhoneUsageAnalyzer.BANSOOGI_PACKAGE
            ) {
                return true
            }

            // 2. 화면 off -> 폰 off 처리
            if (currentEvent.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                return true
            }

            // 3. 앱 off -> 폰 off 처리
            if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED
                || currentEvent.eventType == UsageEvents.Event.ACTIVITY_STOPPED
            ) {
                return true
            }
        }

        return false
    }
}