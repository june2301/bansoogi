package com.ddc.bansoogi.common.data.model

data class ActivityLogDto (
    var type: String = "",
    var fromState: String = "",
    var duration: Int? = null,
    var reactedTime: String = ""
)