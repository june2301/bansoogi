package com.ddc.bansoogi.calendar.data.local

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class Bansoogi : RealmObject {
    @PrimaryKey
    var bansoogiId: Int = 0

    var title: String = ""

    var imageUrl: Int = 0
    var silhouetteImageUrl: Int = 0
    var gifUrl: Int = 0

    var description: String = ""
}