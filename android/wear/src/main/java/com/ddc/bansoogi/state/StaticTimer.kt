package com.ddc.bansoogi.state
// wear/src/main/java/com/ddc/bansoogi/wear/state/StaticTimer.kt

/**
 * 누적 정적(static) 시간을 관리한다.
 *  - onStatic(dtMs) : STATIC 프레임마다 호출하며 dtMs(프레임 간격)만큼 누적.
 *  - reset()       : NON‑STATIC 상태로 전환 시 호출.
 */
class StaticTimer {
    private var accumMs: Long = 0L
    private var lastTs: Long = System.currentTimeMillis()

    fun onStatic() {
        val now = System.currentTimeMillis()
        accumMs += now - lastTs
        lastTs = now
    }

    fun reset() {
        accumMs = 0L; lastTs = System.currentTimeMillis()
    }

    fun accumMs(): Long = accumMs
}