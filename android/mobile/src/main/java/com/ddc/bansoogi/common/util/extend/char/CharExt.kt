package com.ddc.bansoogi.common.util.extend.char

fun Char.isAsciiLetter(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z'

fun Char.isCompleteHangul(): Boolean =
    this in '\uAC00'..'\uD7A3'

fun Char.isAllowedChar(): Boolean =
    isDigit() || isAsciiLetter() || isCompleteHangul()


//TODO: String 확장 추가
//fun String.isValidNickname(min: Int = 3, max: Int = 8): Boolean =
//    this.length in min..max && this.all { it.isAllowedChar() }