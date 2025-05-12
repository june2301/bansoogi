package com.ddc.bansoogi.common.mobile.communication.sender

import android.content.Context
import com.ddc.bansoogi.common.mobile.communication.CommunicationPaths

object MobileMyInfoSender {
    fun send(context: Context) {
        WearToMobileMessageSender.sendReqeust(
            context,
            CommunicationPaths.WearToMobile.MT_INFO_REQUEST
        )
    }

    fun sendToggleNotificationTrigger(context: Context) {
        WearToMobileMessageSender.sendReqeust(
            context,
            CommunicationPaths.WearToMobile.NOTIFICATION_CHANGE_TRIGGER
        )
    }

    fun sendToggleBgSoundTrigger(context: Context) {
        WearToMobileMessageSender.sendReqeust(
            context,
            CommunicationPaths.WearToMobile.BG_SOUND_CHANGE_TRIGGER
        )
    }

    fun sendToggleEffectSoundTrigger(context: Context) {
        WearToMobileMessageSender.sendReqeust(
            context,
            CommunicationPaths.WearToMobile.EFFECT_SOUND_CHANGE_TRIGGER
        )
    }
}