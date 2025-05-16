package com.ddc.bansoogi.common.util.health

import android.util.Log
import com.ddc.bansoogi.common.data.model.TodayRecordModel

class EnergyUtil {
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

        addedEnergy += calculateStep(healthData.step.toInt())
        Log.d("STEP", addedEnergy.toString())
        addedEnergy += calculateFloorsClimbed(healthData.floorsClimbed.toInt())
        Log.d("floorsClimbed", addedEnergy.toString())
        addedEnergy += calculateExercise(healthData.exerciseTime)
        Log.d("exerciseTime", addedEnergy.toString())
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

    companion object

}
