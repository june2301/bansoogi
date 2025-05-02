package com.example.eggi.myInfo.view

import com.example.eggi.myInfo.data.model.MyInfoDto

interface MyInfoView {
    fun displayMyInfo(myInfoDto: MyInfoDto)
}
