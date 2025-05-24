## 1. GitLab 소스 클론 이후 빌드·배포 가이드

> **목표**: 새 기기(Windows 11)에 소스만 클론해도 **모바일·웨어 APK**를 재현‑빌드할 수 있도록 안내
> **조건**
>
> - CI/CD · 원격 서버 · 환경 변수 **없음**
> - 로컬 Realm DB(앱 내부 저장) → 외부 DB 접근/계정 불필요

| 구분               | 내용                                                                                                                                                                                                                                                      |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **IDE**            | Android Studio **Meerkat 2024.3.1 Patch 2**<br/>Build #AI‑243.24978.46.2431.13363775 (2025‑04‑16)<br/>Kotlin plugin K2 mode                                                                                                                               |
| **JDK**            | 내장 OpenJDK 21(IDE와 함께 설치)                                                                                                                                                                                                                          |
| **그래들**         | Wrapper 사용(**`./gradlew`**).<br/>최초 실행 시 자동으로 8.7+ 다운로드                                                                                                                                                                                    |
| **프로젝트 구조**  | `:mobile` 모듈 (휴대폰 APK)<br/>`:wear` 모듈 (워치 APK)                                                                                                                                                                                                   |
| **안드로이드 SDK** | - `compileSdk 35`<br/>- `minSdk 30` (Wear 포함)<br/>SDK 경로는 로컬의 **`local.properties`** → 예시: `sdk.dir=C:\\Users\\SSAFY\\AppData\\Local\\Android\\Sdk`                                                                                             |
| **언어/타겟**      | Kotlin /JVM 17 , Compose UI                                                                                                                                                                                                                               |
| **빌드 명령**      | 1) **전체 클린 빌드**<br/>`bash` `./gradlew clean :mobile:assembleRelease :wear:assembleRelease`<br/>2) **개별 모듈 디버그**<br/>`bash` `./gradlew :mobile:installDebug   # 휴대폰에 연결된 경우` `./gradlew :wear:installDebug     # 워치에 연결된 경우` |
| **프로가드**       | Release 빌드에서 `minifyEnabled false` (난독화 없음)                                                                                                                                                                                                      |
| **출력물**         | - `mobile/build/outputs/apk/release/mobile-release.apk`<br/>- `wear/build/outputs/apk/release/wear-release.apk`                                                                                                                                           |
| **설치 방법**      | 1. USB / ADB Wireless로 기기 연결<br/>2. `bash adb -s <serial> install -r mobile-release.apk`<br/>  워치는 동일 명령 또는 `adb -s <watch-serial> install -r wear-release.apk`                                                                             |
| **디버깅 포인트**  | - **권한**: 알림·Health 센서 관련 권한은 런타임 요청/설정 필수<br/>- **Developer Options**: Wear OS → ADB Debugging ON                                                                                                                                    |
| **특이 사항**      | - **Realm Kotlin 3.0.0**: 최초 실행 시 자동 스키마 생성<br/>- 서버·CI/CD 없음 → APK 사이드로드만으로 시연 가능                                                                                                                                            |

### 1.1 모듈별 핵심 설정 파일 설명

| 파일                      | 위치          | 요약                                                                                                               |
| ------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------ |
| `settings.gradle.kts`     | 루트          | `:mobile`, `:wear` 포함                                                                                            |
| `build.gradle.kts`(루트)  | 루트          | 공통 플러그인 정의, Realm Gradle plugin 등록                                                                       |
| `mobile/build.gradle.kts` | `mobile/`     | `wearAppUnbundled = true`(존재 시)<br/>→ 휴대폰 APK가 워치 APK 번들 포함 여부 결정<br/>(현재 `wear` 모듈 별도 APK) |
| `wear/build.gradle.kts`   | `wear/`       | Wear 전용 SDK·권한·센서 종속성 정의                                                                                |
| `gradle.properties`       | 루트          | JVM 메모리(Xmx2048m), AndroidX 옵션 등                                                                             |
| `local.properties`        | 루트(개별 PC) | **절대 VCS 커밋 금지**. `sdk.dir=...` 만 포함                                                                      |

---

## 2. 외부 SDK·서비스 정보

| 구분        | SDK / Service                                                  | 적용 모듈    | 적용 파일·방법                                                                                                                                                                                         | 개발자 모드 활성화                                                                                                                                                                                  |
| ----------- | -------------------------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1**       | **Samsung Health Data SDK**                                    | **`mobile`** | - `libs/SamsungHealthDataSDK.aar` 추가<br/>- `build.gradle.kts`에 <br/>`kotlin` `implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))`                                       | **휴대폰**<br/>1. *Samsung Health* 앱 실행 → _About Samsung Health_ 진입<br/>2. 버전 영역을 **10회 탭** → *Developer mode* ON<br/>3. `Developer Mode (new)`에서 `Developer Mode for Data Read` _ON_ |
| **2**       | **Samsung Health Sensor SDK**                                  | **`wear`**   | - `libs/SamsungHealthSensorSDK.aar` 추가(동일 방식)<br/>- 매니페스트 권한:<br/>`com.samsung.android.permission.health.MONITOR_PPG`<br/>`com.samsung.android.service.health.permission.HEALTH_TRACKING` | **워치**<br/>1. _Settings → Application → Software version_ 연속 **7회 탭** → _Developer Mode_ 활성화                                                                                               |
| **계정/키** | 특별한 API Key 없음(2025‑05 기준 **Trial 모드** 작동) **주의** |              | 두 SDK는 **정식 배포 키**를 받지 않은 “개발자 전용” 빌드입니다.                                                                                                                                        | 심사·배포 시 Samsung 헬스팀 승인이 필요합니다                                                                                                                                                       |

---

### 2.1 모듈별 코드 참고

| 모듈     | SDK 진입점                                                                                      |
| -------- | ----------------------------------------------------------------------------------------------- |
| `mobile` | `com.ddc.bansoogi.mobile.health.HealthDataManager`<br/>– Data SDK Initialize & Permission Check |
| `wear`   | `com.ddc.bansoogi.sensor.SensorForegroundService`<br/>– Sensor SDK (Continuous PPG) Start/Stop  |

---

## 부록 A. 자주 묻는 질문

| Q                                     | A                                                                                                                                                                        |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **APK가 2개인데 자동 페어링 되나요?** | 아니요. 모바일 APK와 워치 APK를 **각각** 설치해야 합니다(`wearAppUnbundled = true` 설정). 설치 후 첫 실행 시 `Data Layer API`를 통해 자동으로 기기 간 연결을 시도합니다. |
| **AAB로 배포 가능?**                  | Play Console 배포를 계획한다면 **`bundleRelease`** 빌드를 추가하면 됩니다. 현재 포팅 매뉴얼은 APK 사이드로드 기준입니다.                                                 |
| **CI/CD 파이프라인이 필요한가요?**    | 현재 범위에서는 **필요 없음**. 로컬에서 `./gradlew` 빌드 후 APK만 전달하면 됩니다.                                                                                       |

---

### ⬢ 최종 체크리스트

- [ ] Android Studio Meerkat 설치 및 JDK 21 확인
- [ ] `sdk.dir` 수정 후 `./gradlew clean build` 성공
- [ ] `mobile‑release.apk` · `wear‑release.apk` 생성 확인
- [ ] 휴대폰·워치에서 **Developer mode** + Samsung Health SDK 토글 ON
- [ ] APK 사이드로드 후 앱 정상 실행 & 데이터 동기화 확인

> 삼성청년SW·AI아카데미 12기 자율 3반 2팀 원기훈
> lasnier@naver.com / amorinael@gmail.com
> 추가 문의가 있으면 언제든 말씀해 주세요!
