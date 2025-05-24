package com.ddc.bansoogi.common.util.vibrate

enum class VibrationPatternType {
    NUDGE,
    SUCCESS,
    WARNING,
    HAPPY
}

object VibrationPatterns {
    fun getPattern(type: VibrationPatternType): Pair<LongArray, IntArray?> {
        return when (type) {
            VibrationPatternType.NUDGE ->
                Pair(longArrayOf(100, 100, 100, 200), intArrayOf(110, 0, 100, 220))
            VibrationPatternType.SUCCESS ->
                Pair(longArrayOf(0, 50, 50, 50, 50, 100), intArrayOf(0, 150, 0, 150, 0, 200))
            VibrationPatternType.WARNING ->
                // 점점 강해지는 진동 (주의 → 경고)
                Pair(longArrayOf(0, 200, 200, 200, 200, 200, 200, 1000), intArrayOf(0, 80, 0, 160, 0, 220, 0, 255))
            VibrationPatternType.HAPPY ->
                Pair(longArrayOf(0, 30, 50, 30, 50, 30), intArrayOf(0, 180, 0, 180, 0, 220))
        }
    }
}