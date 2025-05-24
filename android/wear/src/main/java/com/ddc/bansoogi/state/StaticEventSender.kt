package com.ddc.bansoogi.state
// wear/src/main/java/com/ddc/bansoogi/wear/state/StaticEventSender.kt

import android.content.Context
import com.ddc.bansoogi.common.mobile.communication.sender.WearToMobileMessageSender
import com.ddc.bansoogi.common.mobile.communication.CommunicationPaths
import com.google.gson.Gson

@Suppress("FunctionName")
object StaticEventSender {
    private val gson = Gson()

    /** STATIC_WARN & STATIC_BREAK 메세지 전송용 DTO */
    private data class StaticWarnDto(val type: String, val duration: Int)
    private data class StaticBreakDto(val type: String)      // 에너지 제거
    private data class StaticAccumDto(val lying: Int? = null, val sitting: Int? = null)

    fun sendWarn(context: Context, type: String, durationMin: Int) {
        val dto = StaticWarnDto(type, durationMin)
        WearToMobileMessageSender.sendData(context, dto, CommunicationPaths.WearToMobile.STATIC_WARN)
    }

    fun sendBreak(ctx: Context, type: String) {
        WearToMobileMessageSender.sendData(ctx,
            StaticBreakDto(type),
            CommunicationPaths.WearToMobile.STATIC_BREAK
        )
    }

    fun sendAccumTime(context: Context, lyingΔ: Int? = null, sittingΔ: Int? = null) {
        val dto = StaticAccumDto(lyingΔ, sittingΔ)
        WearToMobileMessageSender.sendData(context, dto, CommunicationPaths.WearToMobile.STATIC_ACCUM_TIME)
    }
}