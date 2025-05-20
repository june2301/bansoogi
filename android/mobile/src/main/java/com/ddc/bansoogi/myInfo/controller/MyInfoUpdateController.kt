package com.ddc.bansoogi.myInfo.controller

import android.content.Context
import com.ddc.bansoogi.common.wear.communication.receiver.RequestHandler
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyInfoUpdateController(
    private val onCompleted: (MyInfoDto) -> Unit
) {
    private val model = MyInfoModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun save(input: MyInfoDto, context: Context) {
        scope.launch {
            val updated = model.updateMyInfo(input)
            onCompleted(updated)

            // 워치로 전송
            RequestHandler(context, scope).handleMyInfoRequest()
        }
    }
}
