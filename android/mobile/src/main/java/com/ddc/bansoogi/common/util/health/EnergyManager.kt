package com.ddc.bansoogi.common.util.health

import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.health.EnergyUtil
import com.ddc.bansoogi.main.controller.TodayHealthDataController
import com.ddc.bansoogi.calendar.ui.util.CalendarUtils
import java.time.LocalDate

class EnergyManager(
    private val todayRecordModel: TodayRecordModel = TodayRecordModel(),
    private val healthController: TodayHealthDataController = TodayHealthDataController()
) {
    suspend fun energyRecalcAndSave() {
        val today = todayRecordModel.getTodayRecordSync() ?: return

        val dateStr = CalendarUtils.toFormattedDateString(
            LocalDate.now(),
            LocalDate.now().dayOfMonth
        )
        val healthDto = healthController.getTodayHealthData(dateStr)

        val healthData = CustomHealthData(
            step          = healthDto?.steps?.toLong() ?: 0L,
            stepGoal      = healthDto?.stepGoal          ?: 0,
            floorsClimbed = healthDto?.floorsClimbed?.toFloat() ?: 0f,
            sleepData     = healthDto?.sleepTime,
            exerciseTime  = healthDto?.exerciseTime
        )

        val newEnergy = EnergyUtil.calculateEnergyOnce(healthData)

        todayRecordModel.updateAllEnergy(today.recordId, newEnergy)
    }
}
