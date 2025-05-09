package com.ddc.bansoogi.common.util.time

/**
 * 사용자가 입력한 4자리 숫자 문자열을 검증.
 *
 * 입력은 "HHMM" 형태의 문자열로, 시간(HH)은 00~23, 분(MM)은 00~59 범위.
 *
 * 예시:
 * ```
 * validateTime("0930") // true
 * validateTime("2400") // false (24시는 유효하지 않음)
 * validateTime("1180") // false (80분은 없음)
 * validateTime("123")  // false (4자리가 아님)
 * ```
 *
 * @param digits 숫자만 포함된 문자열 (예: 사용자의 키보드 입력에서 숫자만 추출한 값)
 * @return 유효한 시간 형식(HHMM)일 경우 true, 아니면 false
 */
fun validateTime(digits: String): Boolean {
    if (digits.length != 4) return false

    val hour = digits.take(2).toIntOrNull() ?: return false
    val minute = digits.drop(2).toIntOrNull() ?: return false

    return hour in 0..23 && minute in 0..59
}