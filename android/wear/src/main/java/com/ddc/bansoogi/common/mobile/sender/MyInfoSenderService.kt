package com.ddc.bansoogi.common.mobile.sender

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

fun sendRequestMyInfoToMobile(context: Context) {
    val nodeClient = Wearable.getNodeClient(context)
    val messageClient = Wearable.getMessageClient(context)

    nodeClient.connectedNodes.addOnSuccessListener { nodes ->
        if (nodes.isEmpty()) {
            return@addOnSuccessListener
        }

        nodes.forEach { node ->
            messageClient.sendMessage(node.id, "/my_info_request", byteArrayOf())
                .addOnSuccessListener {
                    Log.d("WearDebug", "모바일 요청 전송 성공: ${node.displayName}")
                }
                .addOnFailureListener {
                    Log.e("WearDebug", "요청 전송 실패", it)
                }
        }
    }
}