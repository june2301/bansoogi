package com.example.prototype

/**
 * Real-time stair-climbing detector based on barometer + step gate.
 * This is a direct Kotlin translation of the design in the spec.
 */
class StairUpDetector(
    private val floorHeight: Float = 3.0f, // metres
    private val windowMillis: Long = 3_000L,
    private val minSteps: Int = 3,
) {
    private var alt0: Float? = null
    private var windowStart: Long = 0L
    private var stepCount: Int = 0

    /** 하루 누적 층수 */
    var cumFloor: Int = 0
        private set

    /**
     * Feed one tick of data.
     * @param timestamp   current time in millis
     * @param curAlt      low-pass-filtered altitude in metres
     * @param stepEvt     true if a step event occurred during this tick
     * @return true when a "stair up" event is confirmed
     */
    fun onSensorTick(
        timestamp: Long,
        curAlt: Float,
        stepEvt: Boolean,
    ): Boolean {
        // Initialise reference altitude at first call
        if (alt0 == null) {
            alt0 = curAlt
            windowStart = timestamp
        }

        // 1) Count steps and accumulate altitude difference only when steps occur
        if (stepEvt) {
            stepCount++
            val deltaAlt = curAlt - (alt0 ?: curAlt)
            if (deltaAlt >= floorHeight && stepCount >= minSteps) {
                cumFloor += 1
                // reset reference
                alt0 = curAlt
                stepCount = 0
                windowStart = timestamp
                return true
            }
        }

        // 2) Reset window if expired or descending
        val deltaAltFromBase = curAlt - (alt0 ?: curAlt)
        if (timestamp - windowStart > windowMillis || deltaAltFromBase < 0) {
            alt0 = curAlt
            stepCount = 0
            windowStart = timestamp
        }
        return false
    }

    /** Resets cumulative count (e.g., at midnight) */
    fun resetDailyCount() {
        cumFloor = 0
    }
}