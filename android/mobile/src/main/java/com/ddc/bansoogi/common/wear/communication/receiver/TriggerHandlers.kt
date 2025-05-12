package com.ddc.bansoogi.common.wear.communication.receiver

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TriggerHandlers(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleInteractionTrigger() {
        // TODO: 상호작용 데이터 처리

        Log.d("Mobile Receiver", "상호작용 트리거 발생")
    }

    fun handleToggleNotificationTrigger() {
        handleToggleTrigger { toggleNotification() }
    }

    fun handleToggleBgSoundTrigger() {
        handleToggleTrigger { toggleBgSound() }
    }

    fun handleToggleEffectSoundTrigger() {
        handleToggleTrigger { toggleEffect() }
    }

    fun handleToggleTrigger(
        toggleAction: suspend MyInfoModel.() -> Unit
    ) {
        scope.launch {
            MyInfoModel().toggleAction()
            RequestHandler(context, scope).handleMyInfoRequest()
        }
    }
}