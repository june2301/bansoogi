package com.ddc.bansoogi.common.wear.communication.receiver

import android.content.Context
import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.wear.data.mapper.JsonMapper
import com.ddc.bansoogi.main.ui.handle.handleInteraction
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
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

            model.updateEnergy(today.recordId, 10)
            model.markMealDone(today.recordId, mealType)

            RequestHandler(context, scope).handleTodayRecordRequest()
            RequestHandler(context, scope).handleEnergyRequest()
        }
    }
}