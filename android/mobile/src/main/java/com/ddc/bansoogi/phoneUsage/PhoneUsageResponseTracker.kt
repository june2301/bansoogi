package com.ddc.bansoogi.phoneUsage

import android.app.usage.UsageEvents
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PhoneUsageResponseTracker {
    
    var notificationTime: Long = 0
    var hasAddEnergy: Boolean = false

    fun startMonitoring(context: Context, scope: CoroutineScope, onReward: suspend () -> Unit) {
        notificationTime = System.currentTimeMillis()
        hasAddEnergy = false

        scope.launch {
            repeat(60) { // 60번
                delay(5000) // 5초

                if (isPhoneOff(context)) {
                    if (!hasAddEnergy) { // 이미 에너지 점수 부여 했는지 확인
                        hasAddEnergy = true

                        onReward() // 에너지 추가
                    }
                    return@launch
                }
            }
        }
    }

    fun isPhoneOff(context: Context): Boolean {
        // 알림 발생 부터 현재 이벤트 확인
        val now = System.currentTimeMillis()
        val events = PhoneUsageAnalyzer.getUsageEventsBetween(context, notificationTime, now)

        val currentEvent = UsageEvents.Event()

        while(events.hasNextEvent()) {
            events.getNextEvent(currentEvent)
            
            if (currentEvent.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE
                && currentEvent.packageName == context.packageName
            ) {
                // 반숙이 앱에 접속 -> 폰 off 처리
                return true
            } else if (currentEvent.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                // 화면 off -> 폰 off 처리
                return true
            } else if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                // 앱 off -> 폰 off 처리
                return true
            }
        }

        return false
    }
}