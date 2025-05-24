package com.ddc.bansoogi.phoneUsage

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlin.collections.isNotEmpty

object PhoneUsagePermissionUtil {

    // 사용량 권한은 시스템에서 직접 권환 조회가 불가능 -> 보안 이유
    // 데이터 요청 후, 반환 결과에 따라 권한 상태 간접 추론
    fun hasUsageStatsPermission(context: Context): Boolean {
        // UsageStatsManager 인스턴스 호출
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // 오늘 하루 동안의 앱 사용량을 조회
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, // 모든 앱들의 사용량 정보
            now - 1000 * 60, // 1분 전
            now // 현재 시각
        )

        // 사용량 액세스 권한이 없으면 빈 리스트 or null 반환
        return stats != null && stats.isNotEmpty()
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        // 설정앱으로 이동
        context.startActivity(intent)
    }
}