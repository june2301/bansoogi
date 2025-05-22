package com.ddc.bansoogi.state

import android.util.Log
import java.util.ArrayDeque

/**
 * Sliding‑window tracker that measures the **ratio of static frames** within a recent time window.
 *
 * Improvements over the previous version:
 * 1.  **Window‑readiness guard** – the caller can check `isWindowReady()` (or rely on `staticRatio()`
 *     returning `0.0`) so that a single frame can no longer trigger a spurious 100 % ratio.
 * 2.  **Configurable coverage** – `minCoverageRatio` (default 0.8) lets you decide what fraction of the
 *     target span must be filled before the window is considered ready.
 * 3.  Minor refactor (private `evictOld()` helper) for clarity.
 *
 * @param windowDurationMs  Target span of the sliding window **in milliseconds**.
 * @param minCoverageRatio  Fraction (0‑1) of `windowDurationMs` that must be covered before the window
 *                          is deemed *ready*.
 */
class SlidingWindowStaticTracker(
    private val windowDurationMs: Long,
    private val minCoverageRatio: Double = 0.92,   // 80 % 채워졌을 때부터 유효
) {
    private val states = ArrayDeque<Pair<Long, Boolean>>() // (timestampMs, isStatic)

    /**
     * Add the latest frame state and evict old entries outside the window.
     */
    fun update(isStatic: Boolean) {
        val now = System.currentTimeMillis()
        states.addLast(now to isStatic)
        android.util.Log.d("SlidingWindow", "add: $isStatic at $now, total=${states.size}")
        evictOld(now)
    }

    /**
     * Indicates whether the sliding window already contains *enough* data to make a decision.
     *
     * The window is considered **ready** if either condition is met:
     * 1.  The time span covered by the buffer is at least `windowDurationMs * minCoverageRatio`.
     * 2.  The oldest entry is older than `windowDurationMs` (i.e. the window is completely full).
     */
    fun isWindowReady(): Boolean {
        if (states.isEmpty()) return false
        val now = System.currentTimeMillis()
        val covered = now - states.first.first
        return covered >= windowDurationMs * minCoverageRatio
    }

    /**
     * Returns the **proportion** of frames in the window that were static.
     *
     * If the window is *not yet ready*, this returns `0.0` so that callers can safely compare the
     * result with a threshold (e.g. `>= 0.95`) without triggering false positives.
     */
    fun staticRatio(): Double {
        if (!isWindowReady()) return 0.0
        val staticCount = states.count { it.second }
        val ratio = staticCount.toDouble() / states.size
        Log.d("SlidingWindow", "staticRatio = $ratio (${staticCount}/${states.size})")
        return staticCount.toDouble() / states.size
    }

    /**
     * Clears the tracked history (e.g. on non‑static transition).
     */
    fun reset() = states.clear()

    // ─────────────────────── Helpers ───────────────────────
    private fun evictOld(now: Long) {
        while (states.isNotEmpty() && now - states.first.first > windowDurationMs) {
            states.removeFirst()
        }
    }
}
