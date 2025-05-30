package com.ddc.bansoogi.phoneUsage

import android.content.Context
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.foreground.NotificationDurationStateHolder
import com.ddc.bansoogi.common.notification.NotificationDispatcher
import com.ddc.bansoogi.common.notification.NotificationFactory
import com.ddc.bansoogi.common.wear.communication.receiver.RequestHandler
//import com.ddc.bansoogi.common.wear.communication.sender.SimulateStaticSender
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PhoneUsageMonitor {
    fun monitorContinuousPhoneUsage(context: Context, scope: CoroutineScope) {
        scope.launch {
            while (true) {
                // 현재 핸드폰 사용중 인지 확인
                if (PhoneUsageAnalyzer.isPhoneInUseNow(context)) {
                    // 폰 추적 중인 상태가 아님 -> 새로 추적 시작
                    if (!PhoneUsageAnalyzer.isTrackingUsage()) {
                        PhoneUsageAnalyzer.startUsageTracking()
                    }

                    // 반숙이 애니메이션 변경, BASIC 상태에서만 PHONE으로 변경
                    if (BansoogiStateHolder.state == BansoogiState.BASIC) {
                        BansoogiStateHolder.updateWithWatch(context, BansoogiState.PHONE)
                    }

                    // 기준 이상 사용 확인
                    if (PhoneUsageAnalyzer.isPhoneUsedOverThreshold(NotificationDurationStateHolder.notificationDuration)) {
                        // 핸드폰 사용 시간
                        val time = PhoneUsageAnalyzer.getPhoneUsageMinutes()
//                        val time = 1 // 시연용 시간 알림 출력용 시간 설정

                        // 측정 시간 리셋
                        PhoneUsageAnalyzer.resetUsageStartTime()

                        // 핸드폰 사용 시간 증가
                        TodayRecordModel().updatePhoneTime(time)

                        // 알림 발생
                        NotificationDispatcher.show(
                            context,
                            NotificationDispatcher.Id.PHONE,
                            NotificationFactory.phoneUsage(context, time)
                        )

                        // 핸드폰 off 모니터링 시작
                        PhoneUsageFinishTracker.startMonitoring(context, scope) {
                            // 에너지 증가
                            PhoneUsageEnergyUtil.addEnergy(time)

                            // 알림 발생
                            NotificationDispatcher.show(
                                context,
                                NotificationDispatcher.Id.REWARD,
                                NotificationFactory.cheer(context)
                            )

//                            SimulateStaticSender.simulateStatic(
//                                context = context,
//                                scope   = scope,
//                                type    = "LYING",
//                                delayMs = 8_000L,
//                            )

                        }

                        // 워치로 결과 전송
                        RequestHandler(context, scope).handleTodayRecordRequest()
                    }
                } else {
                    // 핸드폰 사용 중이 아니면 측정 리셋, PHONE에서만 BASIC으로 변경
                    if (BansoogiStateHolder.state == BansoogiState.PHONE) {
                        BansoogiStateHolder.updateWithWatch(context, BansoogiState.BASIC)
                    }

                    PhoneUsageAnalyzer.resetUsageStartTime() // 측정 시간 리셋
                }

                delay(5 * 1000) // 5초마다 체크
            }
        }
    }
}