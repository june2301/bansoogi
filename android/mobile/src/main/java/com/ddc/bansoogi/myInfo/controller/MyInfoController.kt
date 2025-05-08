package com.ddc.bansoogi.myInfo.controller

import android.content.Context
import com.ddc.bansoogi.myInfo.data.local.MyInfoDataSource
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

class MyInfoController {
    private val dataSource = MyInfoDataSource()
    private val model = MyInfoModel()
    private val scope = CoroutineScope(Dispatchers.IO)

    // UI가 구독할 Flow
    fun myInfoFlow(): Flow<MyInfoDto> = model.getMyInfo()

    // 더미 데이터 초기 삽입
    fun initialize(ctx: Context) {
        scope.launch { dataSource.initialize(ctx) }
    }

    // 토글 메서드들 — DB만 갱신하면 Flow가 자동 emit
    fun toggleNotification()   { scope.launch { model.toggleNotification() } }
    fun toggleBgSound() { scope.launch { model.toggleBgSound() } }
    fun toggleEffect()  { scope.launch { model.toggleEffect()  } }
}
