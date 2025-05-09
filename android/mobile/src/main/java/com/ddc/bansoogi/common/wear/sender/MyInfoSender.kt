package com.ddc.bansoogi.common.wear.sender

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.common.wear.data.model.WearMyInfoDto
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson

object MyInfoSender {
    private const val PATH = "/my_info"

    fun send(context: Context, myInfoDto: WearMyInfoDto) {
        val json = Gson().toJson(myInfoDto)

        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.w("WearDebug", "연결된 웨어러블 기기가 없습니다")
                return@addOnSuccessListener
            }

            nodes.forEach { node ->
                messageClient.sendMessage(node.id, MyInfoSender.PATH, json.toByteArray())
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