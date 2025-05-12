package com.ddc.bansoogi.common.util.time

import androidx.compose.runtime.MutableState

fun validateOrShowError(
    timeState: MutableState<String>,
    label: String,
    setError: (String) -> Unit,
    onInvalid: () -> Unit
): Boolean {
    val digits = timeState.value.filter { it.isDigit() }

    return if (!validateTime(digits)) {
        val message = if (digits.length != 4)
            "$label 은 4자리로 입력해야 합니다."
        else
            "$label 형식이 잘못되었습니다. (시: 0~23, 분: 0~59)"
        setError(message)
        onInvalid()
        false
    } else {
        true
    }
}