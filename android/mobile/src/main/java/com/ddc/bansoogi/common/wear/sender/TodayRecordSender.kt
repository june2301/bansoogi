package com.ddc.bansoogi.common.wear.sender

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.common.wear.data.model.WearReportDto
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson

object TodayRecordSender {
    private const val PATH = "/today_record"

    fun send(context: Context, reportDto: WearReportDto) {
        val json = Gson().toJson(reportDto)

        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.w("WearDebug", "연결된 웨어러블 기기가 없습니다")
                return@addOnSuccessListener
            }

            nodes.forEach { node ->
                Log.d("WearDebug", "노드 전송 시도: ${node.id}, ${node.displayName}")  // 다섯 번째 로그

                messageClient.sendMessage(node.id, PATH, json.toByteArray())
                    .addOnSuccessListener {
                        Log.d("TodayRecordSender", "전송 성공: ${node.displayName}")
                    }
                    .addOnFailureListener {
                        Log.e("TodayRecordSender", "전송 실패", it)
                    }
            }
        }
    }
}