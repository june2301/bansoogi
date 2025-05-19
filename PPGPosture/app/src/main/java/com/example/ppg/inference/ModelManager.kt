// app/src/main/java/com/example/ppg/inference/ModelManager.kt
package com.example.ppg.inference

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

object ModelManager {
    private val cache = mutableMapOf<String, Interpreter>()

    /**
     * models/<modelName> 을 assets 에서 찾아서
     * Interpreter 로 로드한 뒤 캐시해 둡니다.
     */
    fun get(ctx: Context, modelName: String): Interpreter {
        return cache.getOrPut(modelName) {
            val afd = ctx.assets.openFd("models/$modelName")
            FileInputStream(afd.fileDescriptor).use { fis ->
                val fc: FileChannel = fis.channel
                val buf = fc.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength
                )
                Interpreter(buf)
            }
        }
    }
}
