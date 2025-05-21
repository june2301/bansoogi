package com.ddc.bansoogi.state
// wear/src/main/java/com/ddc/bansoogi/wear/state/ProlongedStaticMonitor.kt

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class ProlongedStaticMonitor(
    private val ctx: Context,
    private val scope: CoroutineScope,
    notificationDurationMin: Int
) {
    enum class Pending { NONE, SITTING, LYING }
    private var pending = Pending.NONE
    private val timer = StaticTimer()
    private val thresholdMs: Long =
        if (notificationDurationMin <= 1) 10_000L          // 10 초
        else (notificationDurationMin * 60_000 * 0.95).toLong()


    fun onStatic(isSitting: Boolean, isLying: Boolean) {
        timer.onStatic()
        if (pending == Pending.NONE && timer.accumMs() >= thresholdMs) {
            if (isSitting) warn("SITTING_LONG") else if (isLying) warn("LYING_LONG")
        }
    }

    fun onNonStatic() {              // 호출: STATIC → NON‑STATIC 전이 시
        timer.reset(); pending = Pending.NONE
    }

    fun complete() {                 // 보상 완료 후 버퍼 해제
        timer.reset(); pending = Pending.NONE
    }

    fun getPending(): Pending = pending

    private fun warn(type: String) {
        val minutes = (timer.accumMs() / 60_000).toInt()
        StaticEventSender.sendWarn(ctx, type, minutes)
        pending = if (type == "SITTING_LONG") Pending.SITTING else Pending.LYING
    }
}