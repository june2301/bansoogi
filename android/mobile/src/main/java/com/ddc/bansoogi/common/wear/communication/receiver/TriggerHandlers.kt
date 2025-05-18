package com.ddc.bansoogi.common.wear.communication.receiver

import android.content.Context
import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.wear.data.mapper.JsonMapper
import com.ddc.bansoogi.main.ui.handle.handleInteraction
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.health.EnergyUtil
import com.ddc.bansoogi.calendar.ui.util.CalendarUtils
import com.ddc.bansoogi.main.data.model.TodayHealthDataDto
import com.ddc.bansoogi.main.controller.TodayHealthDataController
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TriggerHandlers(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleInteractionTrigger() {
        scope.launch {
            handleInteraction()
        }
    }

    fun handleToggleNotificationTrigger() {
        handleToggleTrigger { toggleNotification() }
    }

    fun handleToggleBgSoundTrigger() {
        handleToggleTrigger { toggleBgSound() }
    }

    fun handleToggleEffectSoundTrigger() {
        handleToggleTrigger { toggleEffect() }
    }

    fun handleToggleTrigger(
        toggleAction: suspend MyInfoModel.() -> Unit
    ) {
        scope.launch {
            MyInfoModel().toggleAction()
            RequestHandler(context, scope).handleMyInfoRequest()
        }
    }

    fun handleMealCheckTrigger(rawData: ByteArray) {
        scope.launch {
            val json = String(rawData)

            val mealTypeName: String = JsonMapper.fromJson(json)
            val mealType = MealType.valueOf(mealTypeName)

            val model = TodayRecordModel()
            val today = model.getTodayRecordSync() ?: return@launch

            model.markMealDone(today.recordId, mealType)

            val dateStr = CalendarUtils.toFormattedDateString(
                LocalDate.now(), LocalDate.now().dayOfMonth
            )
            val healthDto: TodayHealthDataDto? =
                TodayHealthDataController().getTodayHealthData(dateStr)
            val healthData = CustomHealthData(
                step           = healthDto?.steps?.toLong() ?: 0L,
                stepGoal       = healthDto?.stepGoal          ?: 0,
                floorsClimbed  = healthDto?.floorsClimbed?.toFloat() ?: 0f,
                sleepData      = healthDto?.sleepTime,
                exerciseTime   = healthDto?.exerciseTime
            )

            val newEnergy = EnergyUtil.calculateEnergyOnce(healthData)
            model.updateAllEnergy(today.recordId, newEnergy)

            RequestHandler(context, scope).handleTodayRecordRequest()
            RequestHandler(context, scope).handleEnergyRequest()
        }
    }
}