package com.ddc.bansoogi.common.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 화면 높이에 비례하는 박스 높이를 계산하여 반환하는 유틸리티 함수
 * @param heightRatio 화면 높이 대비 박스 높이 비율 (기본값: 0.3f)
 * @return 계산된 박스 높이 (Dp)
 */
@Composable
fun calculateBoxHeight(heightRatio: Float = 0.3f): Dp {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    return screenHeight * heightRatio
}