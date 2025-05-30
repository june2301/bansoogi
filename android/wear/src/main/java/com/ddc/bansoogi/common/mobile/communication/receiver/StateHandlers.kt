package com.ddc.bansoogi.common.mobile.communication.receiver

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.common.mobile.data.mapper.JsonMapper
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.myinfo.state.MyInfoStateHolder
import com.ddc.bansoogi.state.StaticEventSender
import kotlinx.coroutines.CoroutineScope

class StateHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleBansoogiStateData(data: ByteArray) {
        val state = BansoogiState.fromString(String(data))

        BansoogiStateHolder.update(state)
    }

//    fun handleSimulateStatic(rawData: ByteArray) {
//        try {
//            val map = JsonMapper.fromJson<Map<String, String>>(String(rawData))
//            val type = map["type"] ?: return
//
//            val warnCode = when (type.uppercase()) {
//                "SITTING" -> "SITTING_LONG"
//                "LYING"   -> "LYING_LONG"
//                else      -> return
//            }
//            val durationMin = MyInfoStateHolder.myInfoDto?.notificationDuration ?: 0
//
//            Log.d("StateHandler", "simulateStatic → type=$warnCode, duration=$durationMin")
//
//            val pending =
//                if (type.uppercase() == "SITTING")
//                    com.ddc.bansoogi.state.ProlongedStaticMonitor.Pending.SITTING
//                else
//                    com.ddc.bansoogi.state.ProlongedStaticMonitor.Pending.LYING
//
//            com.ddc.bansoogi.state.ProlongedStaticMonitorHolder
//                .monitor
//                .simulateWarn(pending)
//
//        } catch (e: Exception) {
//            Log.e("StateHandler", "simulateStatic 실패", e)
//        }
//    }
}