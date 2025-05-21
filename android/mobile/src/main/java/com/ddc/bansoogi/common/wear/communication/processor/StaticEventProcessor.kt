// mobile/src/main/java/com/ddc/bansoogi/common/wear/communication/processor/StaticEventProcessor.kt
package com.ddc.bansoogi.common.wear.communication.processor

import android.content.Context
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.notification.NotificationDispatcher
import com.ddc.bansoogi.common.notification.NotificationDispatcher.Id
import com.ddc.bansoogi.common.notification.NotificationFactory
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object StaticEventProcessor {
    private val gson = Gson()

    /* DTO 정의 ------------------------------------------------ */
    private data class WarnDto  (val type: String, val duration: Int)
    private data class BreakDto (val type: String)            // energy 필드 삭제
    private data class AccumDto (val lying: Int?, val sitting: Int?)

    /* 1) STATIC_WARN ----------------------------------------- */
    fun handleWarn(ctx: Context, scope: CoroutineScope, raw: ByteArray) {
        val dto = gson.fromJson(String(raw), WarnDto::class.java)
        scope.launch {
            val model  = TodayRecordModel()
            val rec    = model.getTodayRecordSync() ?: return@launch

            /* DB 반영 */
            when (dto.type) {
                "SITTING_LONG" -> model.dataSource.updateSittingTime(rec.recordId, dto.duration)
                "LYING_LONG"   -> model.dataSource.updateLyingTime(rec.recordId, dto.duration)
            }

            /* 로그 */
            val from  = if (dto.type == "SITTING_LONG") "SITTING" else "LYING"
            model.logModel.createActivicyLog(
                activityType = from, fromBehavior = from, durationTime = dto.duration
            )

            /* 로컬 알림 */
            val builder = when (dto.type) {
                "SITTING_LONG" -> NotificationFactory.sitting(ctx, dto.duration)
                "LYING_LONG"   -> NotificationFactory.lying(ctx, dto.duration)
                else           -> null
            }
            builder?.let {
                val nid = if (dto.type == "SITTING_LONG") Id.SITTING else Id.LYING
                NotificationDispatcher.show(ctx, nid, it)
            }
        }
    }

    /* 2) STATIC_BREAK ---------------------------------------- */
    fun handleBreak(ctx: Context, scope: CoroutineScope, raw: ByteArray) {
        val dto = gson.fromJson(String(raw), BreakDto::class.java)
        scope.launch {
            val model = TodayRecordModel(); val rec = model.getTodayRecordSync() ?: return@launch

            /* 에너지 +cnt 업데이트 */
            when (dto.type) {
                "STANDUP_REWARD"  -> model.dataSource.updateStandUpCnt(rec.recordId)
                "STRETCH_REWARD"  -> model.dataSource.updateStretchCnt(rec.recordId)
            }

            /* 로그 */
            val activity = if (dto.type == "STANDUP_REWARD") "STANDUP" else "STRETCH"
            val from     = if (dto.type == "STANDUP_REWARD") "SITTING" else "LYING"
            model.logModel.createActivicyLog(activity, from, 0)

            /* 1️⃣ 보상 알림 발송 */
            NotificationDispatcher.show(ctx, Id.REWARD, NotificationFactory.cheer(ctx))

            /* 2️⃣ 워치 속 에기 → SMILE 애니메이션 */
            BansoogiStateHolder.updateWithWatch(ctx, BansoogiState.SMILE)
        }
    }

    /* 3) STATIC_ACCUM_TIME ----------------------------------- */
    fun handleAccum(ctx: Context, scope: CoroutineScope, raw: ByteArray) {
        val dto = gson.fromJson(String(raw), AccumDto::class.java)
        scope.launch {
            val model = TodayRecordModel(); val rec = model.getTodayRecordSync() ?: return@launch
            dto.lying  ?.let { model.dataSource.updateLyingTime  (rec.recordId, it) }
            dto.sitting?.let { model.dataSource.updateSittingTime(rec.recordId, it) }
            /* (원한다면 로그 남기기 생략 가능) */
        }
    }
}
