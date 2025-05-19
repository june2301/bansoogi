package com.ddc.bansoogi.main.ui.handle

import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.util.health.EnergyManager

suspend fun handleMeal(
    todayRecord: TodayRecordDto,
    mealType: MealType
) {
    TodayRecordModel().markMealDone(todayRecord.recordId, mealType)
    EnergyManager().energyRecalcAndSave()
}
