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
                    append("반숙이(이하 “서비스”)는 사용자 여러분의 개인정보를 소중히 다루며, 아래와 같이 개인정보를 처리합니다.\n\n")
                }

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("1. 수집하는 개인정보 항목\n")
                }
                append("• 이메일 주소 (Google OAuth): 회원가입, 로그인\n")
                append("• 닉네임, 생년월일: 프로필 설정, 사용자 구분\n")
                append("• 수면·기상·식사 시간: 알림 설정, 맞춤 에너지 보상\n")
                append("• 센서 데이터: 사용자 행동 분석, 피드백 제공\n")
                append("• 기기 정보: 서비스 품질 개선\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("2. 개인정보 수집 방법\n")
                }
                append("• 사용자가 앱에 직접 입력한 정보\n")
                append("• 삼성 Health SDK, UsageStatsManager, SensorManager API를 통한 자동 수집\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("3. 개인정보 이용 목적\n")
                }
                append("• 맞춤형 건강 미션 및 피드백 제공\n")
                append("• 캐릭터 동기화 및 히스토리 시각화 기능 제공\n")
                append("• 서비스 개선 및 사용자 행동 분석\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("4. 개인정보 보유 및 이용기간\n")
                }
                append("• 탈퇴 시 즉시 자동 파기\n")
                append("• 관계 법령에 따라 보관이 필요한 경우, 해당 기간 동안 보관\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("5. 개인정보 제3자 제공\n")
                }
                append("• 사용자 동의 없이 외부 제공하지 않음 (법령 예외 제외)\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("6. 개인정보 파기 절차 및 방법\n")
                }
                append("• 보관기간 만료 시 즉시 파기\n")
                append("• 전자파일: 복구 불가능한 기술적 방법으로 삭제\n")
                append("• 서면: 분쇄 또는 소각\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("7. 개인정보 보호책임자\n")
                }
                append("• 이름: 윤병희\n")
                append("• 이메일: ddc.manager25@gmail.com\n\n")

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("8. 기타\n")
                }
                append("• 본 방침은 2025년 5월 12일부터 적용됩니다.\n")
                append("• 향후 변경 시 앱 내 공지 및 팝업 등을 통해 안내 예정입니다.\n")
            }
            
            SERVICE -> buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("반숙이 서비스 이용약관\n\n")
                }

                append("1. 목적\n")
                append("이 약관은 \"반숙이\"(이하 '서비스')를 운영하는 RoastedSeaSalt이 제공하는 모바일 및 웨어러블 기반 서비스의 이용과 관련한 조건, 권리, 의무를 규정함을 목적으로 합니다.\n\n")

                append("2. 정의\n")
                append("• “회원”: 본 약관에 동의하고 서비스를 이용하는 자\n")
                append("• “콘텐츠”: 서비스 내 이미지, 캐릭터, 애니메이션, UI 등\n")
                append("• “디바이스 정보”: 센서 데이터(걸음 수, 자세, 화면 사용시간 등)\n\n")

                append("3. 약관 효력 및 변경\n")
                append("• 앱 초기 화면 또는 설정 내 공지로 효력 발생\n")
                append("• 법률 또는 정책 변경 시 사전 고지 후 개정 가능\n\n")

                append("4. 서비스 제공 및 변경\n")
                append("• 실시간 알림, 캐릭터 애니메이션, 에너지 시스템 등 제공\n")
                append("• 일부 기능은 운영상 필요에 따라 변경될 수 있음\n\n")

                append("5. 회원의 의무\n")
                append("• 타인의 정보를 도용하거나 비정상적으로 이용하지 않아야 함\n")
                append("• 앱의 무단 해킹이나 리버스 엔지니어링 금지\n\n")

                append("6. 개인정보 보호\n")
                append("• 개인정보는 별도로 명시된 개인정보 처리방침에 따름\n\n")

                append("7. 지적재산권\n")
                append("• 서비스 및 콘텐츠의 저작권은 RoastedSeaSalt에 있음\n")
                append("• 비상업적 용도를 제외하고 복제·배포 불가\n\n")

                append("8. 책임의 제한\n")
                append("• 디바이스 오작동이나 사용자의 자율행동 결과에 대한 책임 제한\n\n")

                append("9. 해지 및 서비스 종료\n")
                append("• 사용자는 설정 메뉴에서 언제든지 탈퇴 가능\n")
                append("• 서비스 종료 시 1개월 전 공지하며, 데이터는 방침에 따라 처리\n\n")

                append("10. 분쟁 해결\n")
                append("• 본 약관은 대한민국 법률을 따르며, 분쟁 시 관할 법원은 서울중앙지방법원\n")
            }

            HEALTH -> buildAnnotatedString { append("건강정보 수집 및 활용 동의 내용을 여기에 추가하세요.") }
        }
}