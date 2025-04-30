package com.example.prototype

/**
 * Unified high-level state used by the v2 activity pipeline.
 */
enum class ActivityState {
    // Dynamic
    RUNNING,
    WALKING,

    // Static (device not moving)
    STILL,
    // Static
    SITTING,
    STANDING,
    LYING,
}