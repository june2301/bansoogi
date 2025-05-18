package com.example.ppg.inference

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

object ModelManager {
    private val cache = mutableMapOf<String, Interpreter>()

    /** @param name ex) `"ppg_10s_0.tflite"`  */
    fun get(context: Context, name: String): Interpreter =
        cache.getOrPut(name) { load(context, name) }

    private fun load(ctx: Context, assetName: String): Interpreter {
        val afd = ctx.assets.openFd("models/$assetName")
        val bb: MappedByteBuffer =
            FileInputStream(afd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength,
            )
        return Interpreter(bb, Interpreter.Options().apply { setNumThreads(2) })
    }

    fun closeAll() = cache.values.forEach { it.close() }.also { cache.clear() }
}
