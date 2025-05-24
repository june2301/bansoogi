package com.ddc.bansoogi.common.util.vibrate

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission

/**
 * [triggerVibration]
 * 지정된 시간(ms)과 세기(amplitude)로 기기의 진동을 발생시킵니다.
 *
 * ✅ 진동이 필요한 모든 상황에서 사용할 수 있는 범용 유틸 함수입니다.
 *    - Compose 화면, Activity, ViewModel 이벤트 등 다양한 위치에서 호출 가능합니다.
 *
 * 📌 사용 예시:
 * val context = LocalContext.current
 * triggerVibration(context) // 기본값: 200ms, 시스템 기본 세기
 * triggerVibration(context, milliseconds = 500, amplitude = 255) // 사용자 지정
 *
 * @param context       진동을 실행할 Android Context (예: Activity, Composable 등)
 * @param milliseconds  진동 지속 시간 (기본값: 200ms)
 * @param amplitude     진동 세기 (기본값: VibrationEffect.DEFAULT_AMPLITUDE)
 *                      - 0: 무음
 *                      - 1~255: 사용자 지정 세기
 *                      - VibrationEffect.DEFAULT_AMPLITUDE: 시스템 기본값
 */

@RequiresPermission(Manifest.permission.VIBRATE)
fun triggerVibration(
    context: Context,
    milliseconds: Long = 200,
    amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE
) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (vibrator.hasVibrator()) {
        val effect = VibrationEffect.createOneShot(milliseconds, amplitude)
        vibrator.vibrate(effect)
    }
}


/**
 * 아래와 같이 사용하시면 됩니다.
 *         val context = LocalContext.current
 *
 *         triggerVibration(context, VibrationPatternType.WARNING)
 */

// 진동 유틸 함수
@RequiresPermission(Manifest.permission.VIBRATE)
fun triggerVibration(context: Context, type: VibrationPatternType) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return

    val (timings, amplitudes) = VibrationPatterns.getPattern(type)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = amplitudes?.let {
            VibrationEffect.createWaveform(timings, it, -1)
        } ?: VibrationEffect.createWaveform(timings, -1)
        vibrator.vibrate(effect)
    } else {
        vibrator.vibrate(timings, -1) // amplitude 설정 불가한 구버전
    }
}