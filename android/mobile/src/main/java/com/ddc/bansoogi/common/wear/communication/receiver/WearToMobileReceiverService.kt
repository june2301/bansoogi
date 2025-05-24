package com.ddc.bansoogi.common.wear.communication.receiver

import android.util.Log
import com.ddc.bansoogi.common.wear.communication.CommunicationPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class WearToMobileReceiverService: WearableListenerService() {
    // 서비스 수명 주기에 맞춘 코루틴 스코프
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 핸들러
    private lateinit var requestHandler: RequestHandler
    private lateinit var triggerHandlers: TriggerHandlers
    private lateinit var stateHandler: StateHandler

    // 서비스 생성될 때, 핸들러에 객체 주입
    override fun onCreate() {
        super.onCreate()
        requestHandler = RequestHandler(applicationContext, serviceScope)
        triggerHandlers = TriggerHandlers(applicationContext, serviceScope)
        stateHandler = StateHandler(applicationContext, serviceScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 서비스 종료 시 코루틴 취소
    }

    // 메세지 수신 함수
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        // 각 경로에 따라 함수 실행
        when (messageEvent.path) {
            CommunicationPaths.WearToMobile.ENERGY_REQUEST -> requestHandler.handleEnergyRequest()
            CommunicationPaths.WearToMobile.TODAY_RECORD_REQUEST -> requestHandler.handleTodayRecordRequest()
            CommunicationPaths.WearToMobile.MT_INFO_REQUEST -> requestHandler.handleMyInfoRequest()

            CommunicationPaths.WearToMobile.BANSOOGI_ANIMATION -> stateHandler.handleBansoogiStateData(messageEvent.data)

            CommunicationPaths.WearToMobile.INTERACTION_TRIGGER -> triggerHandlers.handleInteractionTrigger()

            CommunicationPaths.WearToMobile.NOTIFICATION_CHANGE_TRIGGER -> triggerHandlers.handleToggleNotificationTrigger()
            CommunicationPaths.WearToMobile.BG_SOUND_CHANGE_TRIGGER -> triggerHandlers.handleToggleBgSoundTrigger()
            CommunicationPaths.WearToMobile.EFFECT_SOUND_CHANGE_TRIGGER -> triggerHandlers.handleToggleEffectSoundTrigger()
            CommunicationPaths.WearToMobile.MEAL_CHECK_TRIGGER -> triggerHandlers.handleMealCheckTrigger(messageEvent.data)

            CommunicationPaths.WearToMobile.STATIC_WARN       -> triggerHandlers.handleStaticWarn(messageEvent.data)
            CommunicationPaths.WearToMobile.STATIC_BREAK      -> triggerHandlers.handleStaticBreak(messageEvent.data)
            CommunicationPaths.WearToMobile.STATIC_ACCUM_TIME -> triggerHandlers.handleStaticAccum(messageEvent.data)

            else -> Log.w("WatchReceiver", "알 수 없는 경로: ${messageEvent.path}")
        }
    }
}