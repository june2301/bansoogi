package com.example.prototype

/**
 * Unified high-level state used by the v2 activity pipeline.
 */
enum class ActivityState {
    // Dynamic
    STAIR_UP,
    RUNNING,
    WALKING,
    EXERCISE,
    DYNAMIC_GENERIC,

    // Static
    SITTING,
    STANDING,
    LYING,

    // Transitional / Unknown
    TRANSIENT,
}
