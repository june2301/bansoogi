package com.ddc.bansoogi.common.util.health

import com.ddc.bansoogi.common.data.model.TodayRecordModel

object EnergyUtil {
    // 처음 Energy 점수 계산
    fun calculateEnergyOnce(healthData: CustomHealthData): Int {
        var addedEnergy = 0

        val todayRecordDto = TodayRecordModel().getTodayRecordSync()

        if (todayRecordDto?.breakfast == true) {
            addedEnergy += 10
        }
        if (todayRecordDto?.lunch == true) {
            addedEnergy += 10
        }
        if (todayRecordDto?.dinner == true) {
            addedEnergy += 10
        }

        addedEnergy += (todayRecordDto?.interactionCnt?.times(5) ?: 0)

        addedEnergy += (todayRecordDto?.phoneOffCnt?.times(10) ?: 0)
        addedEnergy += (todayRecordDto?.standUpCnt?.times(10) ?: 0)
        addedEnergy += (todayRecordDto?.stretchCnt?.times(10) ?: 0)

        addedEnergy += calculateStep(healthData.step.toInt())
        addedEnergy += calculateFloorsClimbed(healthData.floorsClimbed.toInt())
        addedEnergy += calculateExercise(healthData.exerciseTime)

        return addedEnergy
    }

    fun calculateStep(step: Int?) : Int {
        step?.let {
            return (step / 1_000) * 5
        } ?: return 0
    }

    fun calculateFloorsClimbed(floors: Int?) : Int {
        floors?.let {
            return (floors / 5) * 10
        } ?: return 0
    }

    fun calculateExercise(exercise: Int?) : Int {
        exercise?.let {
            return (exercise / 15) * 30
        } ?: return 0
    }
}
