package com.ddc.bansoogi.common.mobile.communication.sender

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson

object WearToMobileMessageSender {
    fun sendReqeust(
        context: Context,
        path: String
    ) {
        sendData(context, null, path)
    }

    fun <T> sendData(
        context: Context,
        data: T?,
        path: String
    ) {
        // 데이터 변환 (null이면 빈 바이트 배열)
        val dataBytes = if (data != null) {
            Gson().toJson(data).toByteArray()
        } else {
            byteArrayOf()
        }

        // 연결된 Mobile 가져오기
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.w("WearDebug", "연결된 웨어러블 기기가 없습니다")
                return@addOnSuccessListener
            }

            // 모든 연결된 Wear OS 기기에 메시지 전송
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, dataBytes)
                    .addOnSuccessListener {
                        Log.d("WearMessageSender", "전송 성공: ${node.displayName} 경로: $path")
                    }
                    .addOnFailureListener {
                        Log.e("WearMessageSender", "전송 실패", it)
                    }
            }
        }
    }
}