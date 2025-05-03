package com.ddc.bansoogi.myInfo.view

import com.ddc.bansoogi.myInfo.data.model.MyInfoDto

interface MyInfoView {
    fun displayMyInfo(myInfoDto: MyInfoDto)
}
