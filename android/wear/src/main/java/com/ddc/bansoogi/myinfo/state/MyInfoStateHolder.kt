package com.ddc.bansoogi.myinfo.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto

object MyInfoStateHolder {
    var myInfoDto by mutableStateOf(MyInfoDto.default())

    fun update(newDto: MyInfoDto?) {
        myInfoDto = newDto ?: MyInfoDto.default()
    }
}