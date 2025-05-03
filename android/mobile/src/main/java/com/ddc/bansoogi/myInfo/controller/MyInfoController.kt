package com.ddc.bansoogi.myInfo.controller

import com.ddc.bansoogi.myInfo.data.local.MyInfoDataSource
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import com.ddc.bansoogi.myInfo.view.MyInfoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyInfoController(private val view: MyInfoView) {
    private val dataSource = MyInfoDataSource()
    private val model = MyInfoModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        scope.launch {
            dataSource.initialize() // 더미데이터 값 넣어주기
            model.getMyInfo().collectLatest { info ->
                view.displayMyInfo(info)
            }
        }
    }

    fun toggleAlarm() {
        scope.launch {
            val updated = model.toggleAlarm()
            view.displayMyInfo(updated)
        }
    }
    fun toggleBgSound() {
        scope.launch {
            val updated = model.toggleBgSound()
            view.displayMyInfo(updated)
        }
    }
    fun toggleEffect() {
        scope.launch {
            val updated = model.toggleEffect()
            view.displayMyInfo(updated)
        }
    }
}
