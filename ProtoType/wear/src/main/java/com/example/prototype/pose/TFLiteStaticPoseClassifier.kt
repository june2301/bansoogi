package com.example.prototype.pose

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteStaticPoseClassifier(
    private val context: Context,
) {
    companion object {
        private const val TAG = "PoseClassifier"
        private const val MODEL_NAME = "sitlie_v1.tflite"
        private const val WIN_SIZE = 125
    }

    private val interpreter: Interpreter by lazy { createInterpreter() }

    private fun createInterpreter(): Interpreter {
        val assetMgr = context.assets
        val fileDescriptor = assetMgr.openFd(MODEL_NAME)
        val input = fileDescriptor.createInputStream()
        val modelBytes = input.readBytes()
        val byteBuffer =
            ByteBuffer.allocateDirect(modelBytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(modelBytes)
                rewind()
            }
        return Interpreter(byteBuffer)
    }

    /**
     * @param window flattened FloatArray size 125*3
     * @return 0=sitting,1=lying,2=standing
     */
    fun predict(window: FloatArray): Int {
        require(window.size == WIN_SIZE * 3) { "Invalid window size" }
        val inBuffer =
            ByteBuffer.allocateDirect(4 * window.size).apply {
                order(ByteOrder.nativeOrder())
                for (v in window) putFloat(v)
                rewind()
            }
        val outBuffer = ByteBuffer.allocateDirect(4 * 3).apply { order(ByteOrder.nativeOrder()) }
        interpreter.run(inBuffer, outBuffer)
        outBuffer.rewind()
        val probs = FloatArray(3) { outBuffer.float }
        var maxIdx = 0
        var maxVal = probs[0]
        for (i in 1 until probs.size) {
            if (probs[i] > maxVal) {
                maxIdx = i
                maxVal = probs[i]
            }
        }
        Log.d(TAG, "predict probs=${probs.contentToString()} -> $maxIdx")
        return maxIdx
    }

    fun close() {
        interpreter.close()
    }
}
