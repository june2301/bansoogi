package com.ddc.bansoogi.landing.ui.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

enum class AgreementType(val title: String, val content: String) {
    SERVICE("[필수] Bansoogi 서비스 이용약관에 동의합니다.", "[필수]\nBansoogi 서비스 이용약관에 동의합니다."),
    PRIVACY("[필수] 개인정보 처리방침에 동의합니다.", "[필수]\n개인정보 처리방침에 동의합니다."),
    HEALTH("[필수] 삼성 헬스 데이터 연동에 동의합니다.", "[필수]\n삼성 헬스 데이터 연동에 동의합니다.");

    val contentAnnotated: AnnotatedString
        get() = when (this) {
            PRIVACY -> buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("반숙이(이하 “서비스”)는 사용자 여러분의 개인정보를 소중히 여기며, 다음과 같이 개인정보를 처리합니다.\n\n")
                }

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("1. 수집하는 개인정보 항목\n")
                }
                append("• 이메일 주소 (Google OAuth): 회원 가입 및 로그인 목적\n")
                append("• 닉네임, 생년월일: 프로필 설정 및 사용자 식별 목적\n")
                append("• 수면, 기상, 식사 시간: 알림 설정 및 맞춤형 에너지 보상 제공 목적\n")
                append("• 센서 데이터: 사용자 행동 분석 및 피드백 제공 목적\n")
                append("• 기기 정보: 서비스 품질 향상 목적\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("2. 개인정보 수집 방법\n")
                }
                append("• 사용자가 앱에 직접 입력한 정보\n")
                append("• 삼성 Health SDK, UsageStatsManager, SensorManager API 등을 통한 자동 수집\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("3. 개인정보의 이용 목적\n")
                }
                append("• 맞춤형 건강 미션 제공 및 피드백 기능 제공\n")
                append("• 캐릭터 동기화 및 활동 히스토리 시각화 기능 제공\n")
                append("• 서비스 개선을 위한 사용자 행동 분석\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("4. 개인정보 보유 및 이용 기간\n")
                }
                append("• 회원 탈퇴 시 즉시 개인정보를 자동으로 파기합니다.\n")
                append("• 단, 관련 법령에 따라 일정 기간 보관이 필요한 경우 해당 기간 동안 보관합니다.\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("5. 개인정보의 제3자 제공\n")
                }
                append("• 서비스는 사용자의 사전 동의 없이 개인정보를 외부에 제공하지 않습니다. 단, 법령에 의한 경우는 예외로 합니다.\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("6. 개인정보 파기 절차 및 방법\n")
                }
                append("• 보유 기간이 종료된 개인정보는 지체 없이 파기합니다.\n")
                append("• 전자적 파일 형태의 정보는 복구가 불가능한 기술적 방법을 통해 삭제하며, 서면으로 출력된 정보는 분쇄하거나 소각합니다.\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("7. 개인정보 보호책임자\n")
                }
                append("• 성명: 윤병희\n")
                append("• 이메일: ddc.manager25@gmail.com\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("8. 기타\n")
                }
                append("• 본 방침은 2025년 5월 12일부터 적용됩니다.\n")
                append("• 개인정보 처리방침이 변경되는 경우, 변경 사항을 앱 내 공지사항 또는 팝업을 통해 안내드릴 예정입니다.\n")
            }
            
            SERVICE -> buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("반숙이 서비스 이용약관\n\n")
                }

                append("1. 목적\n")
                append("본 약관은 “반숙이”(이하 '서비스')를 운영하는 RoastedSeaSalt가 제공하는 모바일 및 웨어러블 기반 서비스의 이용 조건, 권리, 의무 등을 규정함을 목적으로 합니다.\n\n")

                append("2. 정의\n")
                append("• “회원”이란 본 약관에 동의하고 서비스를 이용하는 자를 말합니다.\n")
                append("• “콘텐츠”란 서비스 내 제공되는 이미지, 캐릭터, 애니메이션, 사용자 인터페이스(UI) 등을 의미합니다.\n")
                append("• “디바이스 정보”란 걸음 수, 자세, 화면 사용 시간 등의 센서 데이터를 포함한 기기 관련 정보를 의미합니다.\n\n")

                append("3. 약관의 효력 및 변경\n")
                append("본 약관은 앱 초기화면 또는 설정 메뉴 내 고지를 통해 효력을 발생합니다.\n")
                append("관련 법률의 제·개정 또는 서비스 운영 정책 변경에 따라 사전 고지를 통해 본 약관을 개정할 수 있습니다.\n\n")

                append("4. 서비스의 제공 및 변경\n")
                append("서비스는 실시간 알림, 캐릭터 애니메이션, 에너지 시스템 등의 기능을 제공합니다.\n")
                append("운영상 필요에 따라 일부 기능은 변경될 수 있습니다.\n\n")

                append("5. 회원의 의무\n")
                append("회원은 타인의 정보를 도용하거나, 서비스를 비정상적으로 이용해서는 안 됩니다.\n")
                append("또한, 앱의 무단 해킹이나 리버스 엔지니어링 등 기술적 침해 행위를 해서는 안 됩니다.\n\n")

                append("6. 개인정보 보호\n")
                append("서비스는 개인정보 보호와 관련하여 별도로 고지한 개인정보 처리방침을 따릅니다.\n\n")

                append("7. 지적재산권\n")
                append("서비스 및 콘텐츠에 대한 모든 지적재산권은 RoastedSeaSalt에 귀속됩니다.\n")
                append("단, 비상업적 목적의 개인 사용을 제외하고는 무단 복제, 배포 등을 할 수 없습니다.\n\n")

                append("8. 책임의 제한\n")
                append("서비스는 디바이스의 오작동이나 사용자의 자율적인 행동 결과로 인한 문제에 대해 책임을 지지 않습니다.\n\n")

                append("9. 해지 및 서비스 종료\n")
                append("회원은 설정 메뉴를 통해 언제든지 서비스 이용을 종료(탈퇴)할 수 있습니다.\n")
                append("서비스 종료 시 최소 1개월 전에 사전 공지를 하며, 회원의 정보는 개인정보 처리방침에 따라 처리됩니다.\n\n")

                append("10. 분쟁 해결\n")
                append("본 약관은 대한민국 법률에 따라 해석되며, 서비스와 관련한 분쟁은 서울중앙지방법원을 전속 관할 법원으로 합니다.\n")
            }

            HEALTH -> buildAnnotatedString { append("건강정보 수집 및 활용 동의 내용을 여기에 추가하세요.") }
        }
}