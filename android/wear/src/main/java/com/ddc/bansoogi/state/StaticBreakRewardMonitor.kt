package com.ddc.bansoogi.state

import android.content.Context
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 경고 후 즉시 행동 변화(Stand‑up / Stretch)를 감지하여 보상을 부여한다.
 * ActivityState 변화를 상위(Processor)에서 전달받아 evaluate() 호출.
 */
class StaticBreakRewardMonitor(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val prolonged: ProlongedStaticMonitor
) {
    fun evaluate(prevSitting: Boolean, prevLying: Boolean, currSitting: Boolean, currLying: Boolean) {
        when (prolonged.getPending()) {
            ProlongedStaticMonitor.Pending.SITTING ->
                if (!currSitting) reward("STRETCH_REWARD")
            ProlongedStaticMonitor.Pending.LYING ->
                if (!currLying) reward("STANDUP_REWARD")
            else -> Unit
        }
    }

    private fun reward(type: String) {
        scope.launch { StaticEventSender.sendBreak(ctx, type, 10) }
        BansoogiStateHolder.updateWithMobile(ctx, BansoogiState.SMILE) // 착용 디바이스 기준
        BansoogiStateHolder.update(BansoogiState.SMILE)
        prolonged.complete()
    }
}