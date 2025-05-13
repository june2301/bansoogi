package com.ddc.bansoogi.common.wear.communication.receiver

import android.content.Context
import com.ddc.bansoogi.main.ui.handle.handleInteraction
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TriggerHandlers(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleInteractionTrigger() {
        scope.launch {
            handleInteraction()
        }
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