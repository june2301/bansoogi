// 파일 위치: mobile/src/main/java/com/ddc/bansoogi/common/mobile/communication/receiver/MobileWearReceiverService.kt

package com.ddc.bansoogi.common.mobile.communication.receiver

import android.util.Log
import android.content.Context
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.common.wear.communication.CommunicationPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MobileWearReceiverService : WearableListenerService() {
    private val TAG = "MobileWearReceiver"

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            CommunicationPaths.WearToMobile.BANSOOGI_ANIMATION -> {
                val stateName = String(messageEvent.data)
                val newState = BansoogiState.fromString(stateName)
                Log.d(TAG, "Received Wear state: $newState")
                // 모바일 로컬 상태 머신 갱신 (Compose 쪽에서 Holder.state.collectAsState() 로 자동 반영)
                BansoogiStateHolder.update(newState)
            }
            else -> Log.w(TAG, "Unknown path: ${messageEvent.path}")
        }
    }
}
