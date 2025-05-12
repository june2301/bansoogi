package com.ddc.bansoogi.common.wear.communication.receiver

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope

class TriggerHandlers(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleInteractionTrigger() {
        // TODO: 상호작용 데이터 처리

        Log.d("Mobile Receiver", "상호작용 트리거 발생")
    }
}