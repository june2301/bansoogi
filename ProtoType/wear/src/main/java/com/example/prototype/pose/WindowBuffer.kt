package com.example.prototype.pose

import java.util.ArrayDeque

/**
 * Maintains a fixed-size sliding window (e.g. 125 samples) with 50 % overlap.
 * Each sample is a triple (ax, ay, az) stored as FloatArray of size 3.
 * When the window is full, [addSample] returns a flattened copy FloatArray (size = windowSize*3).
 */
class WindowBuffer(
    private val windowSize: Int,
) {
    private val deque: ArrayDeque<FloatArray> = ArrayDeque()
    private val stride: Int = windowSize / 2

    fun addSample(
        ax: Float,
        ay: Float,
        az: Float,
    ): FloatArray? {
        deque.addLast(floatArrayOf(ax, ay, az))
        if (deque.size < windowSize) return null
        if (deque.size > windowSize) {
            // should not happen but trim
            while (deque.size > windowSize) deque.removeFirst()
        }
        // Produce flattened copy
        val window = FloatArray(windowSize * 3)
        var idx = 0
        deque.forEach { sample ->
            window[idx++] = sample[0]
            window[idx++] = sample[1]
            window[idx++] = sample[2]
        }
        // Remove first stride samples to obtain 50% overlap
        repeat(stride) { deque.removeFirst() }
        return window
    }
}
