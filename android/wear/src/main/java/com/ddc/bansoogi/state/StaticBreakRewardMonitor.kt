package com.ddc.bansoogi.state

import android.content.Context
import com.ddc.bansoogi.activity.StaticType
import com.ddc.bansoogi.state.ProlongedStaticMonitor.Pending
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.atomic.AtomicBoolean

class StaticBreakRewardMonitor(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val prolonged: ProlongedStaticMonitor,
    private val minActiveMs: Long = 10_000L,     // ✨ 최소 ACTIVE 10초
    private val goalSteps: Int = 15              // ✨ 또는 15걸음
) {
    /* 누계·보상 후보 ------------------------------- */
    private var lyingMs   = 0L
    private var sittingMs = 0L
    private var lastTick  = System.currentTimeMillis()

    private var currentTarget = Pending.NONE
    private val breakSent = AtomicBoolean(false)   // ✨ 추가

    private var activeStartMs: Long? = null
    private var stepCnt = 0

    /* 누적 시간 계산용 ----------------------------------------- */
    private var lastAccumTick = System.currentTimeMillis()

    fun onStepDetected() { stepCnt++ }

    /* WARN 시점 콜백 */
    fun onWarnIssued(type: Pending) {
        currentTarget = type
        stepCnt = 0
        activeStartMs = null
    }

    fun onStaticFrame(type: StaticType?) {
        val now = System.currentTimeMillis()
        val dt  = now - lastAccumTick
        lastAccumTick = now

        when (type) {
            StaticType.LYING   -> lyingMs   += dt
            StaticType.SITTING -> sittingMs += dt
            else -> {}                         // STANDING·UNKNOWN 은 누적 안 함
        }

        // ✨ 로그 추가
        android.util.Log.d("StaticAccum", "lyingMs=${lyingMs}ms, sittingMs=${sittingMs}ms (Δ=${dt}ms)")

        sendAccumIfNeeded()                    // ★ 1 분 넘었는지 검사
    }

    /** 매 프레임 호출 */
    fun tick(isStatic: Boolean, isActive: Boolean) {
        /* ACTIVE 지속 타이머 */
        val now = System.currentTimeMillis()
        if (isActive) {
            if (activeStartMs == null) activeStartMs = now
        } else {
            activeStartMs = null
        }

        /* BREAK 가능? (정적 → 비정적 상태) */
        if (!isStatic && currentTarget != Pending.NONE &&
            prolonged.isRewardWindowActive(currentTarget) &&
            ( (activeStartMs!=null && now-activeStartMs!! >= minActiveMs) ||
                    stepCnt >= goalSteps )
        ) {
            if (breakSent.compareAndSet(false, true)) sendBreak()   // ✨ 1‑회 보장
        }
    }

    /* --------------------------------------------- */
    private fun sendBreak() {
        val code = when (currentTarget) {
            Pending.SITTING -> "STRETCH_REWARD"
            Pending.LYING   -> "STANDUP_REWARD"
            else            -> return
        }
        StaticEventSender.sendBreak(ctx, code)
        currentTarget = Pending.NONE
        stepCnt = 0; activeStartMs = null
        prolonged.complete()                 // pending 초기화
        breakSent.set(false)                 // ✨ 창 닫힘 → 다음 BREAK 가능
    }

    /* 1분(60 000 ms) 단위로 누계(ACCUM) 전송 ---------------- */
    private fun sendAccumIfNeeded() {
        var lyingΔ   = 0
        var sittingΔ = 0
        if (lyingMs   >= 60_000L) { lyingΔ   = (lyingMs   / 60_000L).toInt(); lyingMs   %= 60_000L }
        if (sittingMs >= 60_000L) { sittingΔ = (sittingMs / 60_000L).toInt(); sittingMs %= 60_000L }

        if (lyingΔ > 0 || sittingΔ > 0) {
            StaticEventSender.sendAccumTime(ctx,
                lyingΔ.takeIf { it > 0 },
                sittingΔ.takeIf { it > 0 }
            )
        }
    }

    /* 외부에서 누계 조회가 필요하면 ↓ */
    fun getLyingMs()   = lyingMs
    fun getSittingMs() = sittingMs
}
