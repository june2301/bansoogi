package com.ddc.bansoogi.common.wear.communication.receiver

import android.util.Log
import com.ddc.bansoogi.common.wear.communication.CommunicationPaths
import com.ddc.bansoogi.common.wear.communication.processor.StaticEventProcessor
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Wear 메시지를 직접 수신하여 StaticEventProcessor 로 넘긴다.
 * AndroidManifest.xml에 BIND_LISTENER 권한으로 등록해야 한다.
 */
class StaticEventReceiver : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            CommunicationPaths.WearToMobile.STATIC_WARN ->
                StaticEventProcessor.handleWarn(serviceScope, event.data)

            CommunicationPaths.WearToMobile.STATIC_BREAK ->
                StaticEventProcessor.handleBreak(serviceScope, event.data)

            CommunicationPaths.WearToMobile.STATIC_ACCUM_TIME ->
                StaticEventProcessor.handleAccum(serviceScope, event.data)

            else -> Log.d("StaticEventReceiver", "Unknown path ${event.path}")
        }
    }
}
