package com.example.eggi.myInfo.controller

import com.example.eggi.myInfo.data.model.MyInfoModel
import com.example.eggi.myInfo.data.model.MyInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyInfoUpdateController(
    private val onCompleted: (MyInfo) -> Unit
) {
    private val model = MyInfoModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun save(input: MyInfo) {
        scope.launch {
            val updated = model.updateMyInfo(input)
            onCompleted(updated)
        }
    }
}
