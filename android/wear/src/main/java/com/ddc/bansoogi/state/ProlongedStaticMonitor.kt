package com.ddc.bansoogi.state
// wear/src/main/java/com/ddc/bansoogi/wear/state/ProlongedStaticMonitor.kt

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.myinfo.state.MyInfoStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProlongedStaticMonitor(
    private val ctx: Context,
    private val scope: CoroutineScope,
) {
    enum class Pending { NONE, SITTING, LYING }

    /* ---- 경고/보상 상태 ---- */
    private var pending: Pending = Pending.NONE
    private var rewardDeadlineMs: Long = 0L          // ← ‘warn’ 직후 5 분 만료 타임스탬프

    /* ---- 슬라이딩 윈도우 ---- */
    private var windowLenMin = latestDurationMin().coerceAtLeast(1)
    private var window       = SlidingWindowStaticTracker(40_000L)
    private val threshold    = 0.95

    // 시연용
    fun simulateWarn(type: Pending) {
        warn(type)
    }

    // ────────────────────────────────────────────────
    /** 항상 최신 notificationDuration(분) 반환 */
    private fun latestDurationMin(): Int =
        MyInfoStateHolder.myInfoDto?.notificationDuration ?: 0

    /** duration 값이 바뀌었으면 새 윈도우로 교체 */
    private fun refreshWindowIfNeeded() {
        val newLen = latestDurationMin().coerceAtLeast(1)
        if (newLen != windowLenMin) {
            Log.d("ProlongedStaticMonitor", "🔄 windowLenMin 변경: $windowLenMin → $newLen")
            windowLenMin = newLen
            window = SlidingWindowStaticTracker(40_000L)
        } else {
            Log.d("ProlongedStaticMonitor", "✅ windowLenMin 유지: $windowLenMin")
        }
    }
    // ────────────────────────────────────────────────

    /* 콜백 주입용 */
    var rewardCallback: ((Pending) -> Unit)? = null

    var lastWarnType: Pending = Pending.NONE   // ✨ 추가

    /** 매 프레임 호출 – 정적 여부 전달 */
    fun onStatic(isSitting: Boolean, isLying: Boolean) {
        refreshWindowIfNeeded()                 // ★ 동기화 포인트

        window.update(isSitting || isLying)

        val newType = when {
            isSitting -> Pending.SITTING
            isLying   -> Pending.LYING
            else      -> Pending.NONE
        }

//        if (pending == Pending.NONE                       // 아직 경고 안 보냈고
//            && window.staticRatio() >= threshold          // 비율 충족
//        ) {
//            warn(newType)
//        }
    }

    /** 동적 전이·오프바디 등 → 상태 초기화 */
    fun onNonStatic() = reset()

    /** 보상 완료 후 호출 (StaticBreakRewardMonitor 쪽에서) */
    fun complete() = reset()

    /** 현재 ‘보상 기회’가 살아있는지 검사 */
    fun isRewardWindowActive(type: Pending): Boolean =
        pending == type && System.currentTimeMillis() <= rewardDeadlineMs

    /* ---------- 내부 ---------- */
    private fun warn(type: Pending) {
        if (type == Pending.NONE) return        // safety

        // ① 알림 전송
        lastWarnType = type                    // ✨ 기억
        StaticEventSender.sendWarn(
            ctx,
            if (type == Pending.SITTING) "SITTING_LONG" else "LYING_LONG",
            windowLenMin
        )
        rewardCallback?.invoke(type)      // 파라미터로 타입 전달

        /* NEW: 보상 모니터에 알리기 */
//        scope.launch { rewardCallback?.invoke(type) }   // 콜백으로 처리

        // ② 보상 타이머 5 분 세팅
        pending = type
        rewardDeadlineMs = System.currentTimeMillis() + 5 * 60_000L

        // ③ 윈도우 리셋(계속 누워 있어도 다시 95 % 채우도록)
        window.reset()

        scope.launch {
            delay(5 * 60_000L)
            if (pending == type) {
                Log.d("ProlongedStaticMonitor", "보상 이벤트 실패로 pending 자동 해제: $type")
                reset()
            }
        }
    }

    fun onNonStaticWindowReset() = window.reset()

    private fun reset() {
        window.reset()
        pending = Pending.NONE
        rewardDeadlineMs = 0L
    }
}

