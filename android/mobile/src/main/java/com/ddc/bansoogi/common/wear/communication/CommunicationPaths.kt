package com.ddc.bansoogi.common.wear.communication

object CommunicationPaths {
    // Mobile에서 WearOS로 전송하는 메세지 경로
    object MobileToWear {
        private const val PREFIX = "/mobile_to_wear"

        const val ENERGY_DATA = "$PREFIX/data/energy" // 에너지 데이터를 웨어로 전송
        const val TODAY_RECORD_DATA = "$PREFIX/data/today_record" // 데이터를 웨어로 전송
        const val MY_INFO_DATA = "$PREFIX/data/my_info" // 데이터를 웨어로 전송
    }

    // WearOS에서 Mobile로 전송하는 메세지 경로 -> 모바일에서는 리시브에서 사용되는 경로
    object WearToMobile {
        private const val PREFIX = "/wear_to_mobile"

        const val ENERGY_REQUEST = "${PREFIX}/request/energy" // 에너지 데이터 전송 요청
        const val TODAY_RECORD_REQUEST = "${PREFIX}/request/today_record" // 데이터 전송 요청을 받음
        const val MT_INFO_REQUEST = "${PREFIX}/request/my_info" // 데이터 전송 요청을 받음

        const val INTERACTION_TRIGGER = "${PREFIX}/trigger/interaction" // 상호작용 트리거 전송

        const val NOTIFICATION_CHANGE_TRIGGER = "${PREFIX}/trigger/change/notification" // 알림 설정 변경
        const val BG_SOUND_CHANGE_TRIGGER = "${PREFIX}/trigger/change/bg_sound" // 배경음 설정 변경
        const val EFFECT_SOUND_CHANGE_TRIGGER = "${PREFIX}/trigger/change/effect_sound" // 효과음 설정 변경

        const val MEAL_CHECK_TRIGGER = "${PREFIX}/trigger/meal_check" // 식사 이벤트 수행
    }
}