package com.ddc.bansoogi.phoneUsage

import android.util.Log
import com.ddc.bansoogi.common.data.model.ActivityLogModel
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.util.health.EnergyManager

object PhoneUsageEnergyUtil {

    suspend fun addEnergy(durationTime: Int) {
        val todayModel = TodayRecordModel()
        val logModel = ActivityLogModel()

        val recordId = todayModel.getTodayRecordSync()?.recordId

        if (recordId != null) {
            todayModel.updatePhoneOff(recordId)
            EnergyManager().energyRecalcAndSave()

            logModel.createActivicyLog("PHONE_OFF", "PHONE_IN_USE", durationTime)
        } else {
            Log.d("PhoneUsageEnergyUtil", "recordId가 null입니다. 에너지 추가 실패")
        }
    }
}