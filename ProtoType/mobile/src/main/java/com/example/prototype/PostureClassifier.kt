package com.example.prototype

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object PostureClassifier {
    // Posture heuristics parameters
    private const val PITCH_LYING = 50.0 // deg, |pitch| > 50° 또는 |roll| > 50° → LYING
    private const val ROLL_LYING = 50.0 // deg

    // 고도 기반 앉음/서기 구분 (손목 특성 반영 – 기존보다 약간 넓은 히스테리시스)
    private const val STAND_THRESH_UP = 0.40 // m, 이 값 이상 올라가면 "STANDING"
    private const val STAND_THRESH_DOWN = 0.25 // m, 이 값 이하로 내려오면 "SITTING"

    // 동적 동작(걷기/팔 휘두르기) 시 자세 고정하기 위한 임계값
    private const val DYN_GYRO_THRESH = 100.0 // deg/s, 자이로 합 > 100 이면 동적
    private const val DYN_ACC_THRESH = 2.0 // m/s^2, ||acc|-g| > 2m/s^2

    // STANDING wrist orientation 제한 (팔이 아래로 내려가 있을 때)
    private const val STAND_PITCH_MAX = 35.0 // deg
    private const val STAND_ROLL_MAX = 35.0 // deg

    private const val BASELINE_ALPHA = 0.01 // 기압 기준의 천천히 적응 비율
    private const val SMOOTH_WINDOW = 6 // 지터 완화용 창 크기 (약 1.5s @4Hz)
    private const val LYING_DH_MAX = 0.15 // m, 손목 고도 변동이 15cm 이내일 때만 LYING 후보

    private val lpfAcc = List(3) { LowPassFilter(0.2) }
    private val lpfBaro = LowPassFilter(0.2)
    private val window = ArrayDeque<Posture>()
    private var basePressure: Double? = null // hPa
    private var prevPosture: Posture = Posture.SITTING

    fun classify(
        rawAx: Double,
        rawAy: Double,
        rawAz: Double,
        rawGx: Double,
        rawGy: Double,
        rawGz: Double,
        rawBaro: Double,
    ): Posture {
        // 0. 노이즈 필터링 (저역통과)
        val ax = lpfAcc[0].filter(rawAx)
        val ay = lpfAcc[1].filter(rawAy)
        val az = lpfAcc[2].filter(rawAz)
        val pressure = lpfBaro.filter(rawBaro) // hPa

        // 0-1. 동적 여부 판단 (필터 적용 전 원시 값 사용)
        val gyroMag = sqrt(rawGx * rawGx + rawGy * rawGy + rawGz * rawGz)
        val accMag = sqrt(rawAx * rawAx + rawAy * rawAy + rawAz * rawAz)
        val isDynamic = gyroMag > DYN_GYRO_THRESH || abs(accMag - 9.81) > DYN_ACC_THRESH

        // 1. 중력 기반 pitch/roll 계산 (deg)
        val pitch = Math.toDegrees(atan2(-ax, sqrt(ay * ay + az * az)))
        val roll = Math.toDegrees(atan2(ay, az))

        // 2. 기준 기압 보정: 최초 값 또는 "앉음" 상태에서 천천히 업데이트
        if (basePressure == null) {
            basePressure = pressure
        }

        // 3. 압력 → 상대고도(m) 변환 (대략 Δh ≈ (P0 - P) * 8.3)
        val deltaH = ((basePressure ?: pressure) - pressure) * 8.3 // meters, +면 높아짐

        // 4. 휴리스틱 분류 (우선순위: LYING > STANDING > SITTING)
        val candidate =
            when {
                (abs(pitch) > PITCH_LYING || abs(roll) > ROLL_LYING) && deltaH < LYING_DH_MAX && !isDynamic -> Posture.LYING
                // STANDING 후보: wrist orientation이 비교적 수직 + 고도 충분히 높음
                deltaH >= STAND_THRESH_UP && abs(pitch) < STAND_PITCH_MAX && abs(roll) < STAND_ROLL_MAX -> Posture.STANDING
                // SITTING 판정: 낮은 고도이거나 팔이 들린 상태
                deltaH <= STAND_THRESH_DOWN -> Posture.SITTING
                else -> prevPosture // 중간 영역에서는 이전 상태 유지
            }

        // 5. 지터 완화 (다수결)
        if (window.size >= SMOOTH_WINDOW) window.removeFirst()
        window += candidate
        val smooth =
            window
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }!!
                .key

        // 6. baseline pressure를 SITTING 상태에서 서서히 적응시켜 장기 드리프트 보정
        if (smooth == Posture.SITTING) {
            basePressure =
                BASELINE_ALPHA * pressure + (1 - BASELINE_ALPHA) * (basePressure ?: pressure)
        }

        prevPosture = smooth
        return smooth
    }
}

// 1차 저역 통과 필터
class LowPassFilter(
    private val α: Double,
) {
    private var prev: Double? = null

    fun filter(x: Double): Double {
        val y = α * x + (1 - α) * (prev ?: x)
        prev = y
        return y
    }
}

// 적응형 임계치
class AdaptiveThreshold(
    var threshold: Double,
    val standAlt: Double = 0.0,
) {
    private val sitSamples = mutableListOf<Double>()
    private val standSamples = mutableListOf<Double>()

    fun update(
        p: Posture,
        deltaH: Double,
    ) {
        when (p) {
            Posture.SITTING -> if (deltaH in -0.5..0.5) sitSamples += deltaH
            Posture.STANDING -> if (deltaH in 0.2..1.0) standSamples += deltaH
            else -> {}
        }
        if (sitSamples.size >= 20 && standSamples.size >= 20) {
            val avg = (sitSamples.average() + standSamples.average()) / 2
            threshold = 0.1 * avg + 0.9 * threshold
            sitSamples.clear()
            standSamples.clear()
        }
    }
}