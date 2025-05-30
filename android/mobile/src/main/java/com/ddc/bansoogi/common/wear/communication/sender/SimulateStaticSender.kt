//package com.ddc.bansoogi.common.wear.communication.sender
//
//import android.content.Context
//import com.ddc.bansoogi.common.wear.communication.CommunicationPaths
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
///**
// * 워치에서의 onStatic() 경고를 “강제로” 발생시키고 싶을 때 사용합니다.
// */
//object SimulateStaticSender {
//
//    /**
//     * @param scope      메시지 전송을 위해 사용될 코루틴 스코프
//     * @param type       "LYING" 또는 "SITTING" 문자열
//     * @param delayMs    워치에 전송하기 전 대기 시간 (밀리초)
//     */
//    fun simulateStatic(
//        context: Context,
//        scope: CoroutineScope,
//        type: String,
//        delayMs: Long = 8_000L
//    ) {
//        scope.launch {
//            delay(delayMs)
//            // 간단한 DTO: { "type":"LYING" } 혹은 { "type":"SITTING" }
//            val dto = mapOf("type" to type)
//            MobileToWearMessageSender.sendData(
//                context       = context,
//                data          = dto,
//                path          = CommunicationPaths.MobileToWear.SIMULATE
//            )
//        }
//    }
//}
