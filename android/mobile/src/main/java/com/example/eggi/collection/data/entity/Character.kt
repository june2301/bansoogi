package com.example.eggi.collection.data.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class Character : RealmObject {
    @PrimaryKey
    var bansoogiId: Int = 0

    var title: String = ""
    var imageUrl: String = ""
    var silhouetteImageUrl: String = ""
    var gifUrl: String = ""
    var description: String = ""
}