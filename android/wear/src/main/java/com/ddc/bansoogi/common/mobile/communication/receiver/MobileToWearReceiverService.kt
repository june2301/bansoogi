package com.ddc.bansoogi.common.mobile.communication.receiver

import android.util.Log
import com.ddc.bansoogi.common.mobile.communication.CommunicationPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MobileToWearReceiverService : WearableListenerService() {
    // 서비스 수명 주기에 맞춘 코루틴 스코프
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 핸들러
    private lateinit var requestHandler: RequestHandler
    private lateinit var stateHandler: StateHandler

    // 서비스 생성될 때, 핸들러에 객체 주입
    override fun onCreate() {
        super.onCreate()
        requestHandler = RequestHandler(applicationContext, serviceScope)
        stateHandler = StateHandler(applicationContext, serviceScope)
    }

    // 메세지 수신 함수
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        // 각 경로에 따라 함수 실행
        when (messageEvent.path) {
            CommunicationPaths.MobileToWear.ENERGY_DATA -> requestHandler.handleEnergyData(messageEvent.data)
            CommunicationPaths.MobileToWear.TODAY_RECORD_DATA -> requestHandler.handleTodayRecordData(messageEvent.data)
            CommunicationPaths.MobileToWear.MY_INFO_DATA -> requestHandler.handleMyInfoData(messageEvent.data)

            CommunicationPaths.MobileToWear.BANSOOGI_ANIMATION -> stateHandler.handleBansoogiStateData(messageEvent.data)

            CommunicationPaths.MobileToWear.SIMULATE -> stateHandler.handleSimulateStatic(messageEvent.data)

            else -> Log.w("WatchReceiver", "알 수 없는 경로: ${messageEvent.path}")
        }
    }
}