# Prototype – Galaxy Watch 기반 실시간 자세 인식 데모

> Wear OS 4 (Galaxy Watch 7) ↔︎ Android 15 (Galaxy S22) 간 BLE(Data-Layer) 통신으로 **앉음 / 서있음 / 누움** 자세를
≤120 ms 지연으로 실시간 추정하는 PoC 애플리케이션입니다.  
> 모든 코드는 **Kotlin DSL · API 35 · Phone & Wear 2 모듈**을 기준으로 작성되었습니다.

## 1. 시스템 아키텍처

```
[Galaxy Watch 7]                   │    [Galaxy S22]
┌──────────────────────────────┐   │   ┌──────────────────────────────┐
│ ProtoWearSensorService (Wear)│   │   │ ProtoBleReceiverService      │
│  • 25 Hz ACC/GYRO            │BLE│   │  (Foreground Service)        │
│  • 10 Hz BARO (hPa)          ├──►│   │  • 메시지 수신                 │
│  • 250 ms마다 패킷 전송        │   │   │  • PostureClassifier.classify│
└──────────────────────────────┘   │   │  • LiveData<Posture> export  │
                                   │   └─────────────┬────────────────┘
                                   │                 │
                                   │      collectAsState()
                                   ▼                 │
                          ┌────────────────────────────────────────┐
                          │ MainActivity (Jetpack Compose UI)      │
                          │  "앉음 / 서있음 / 누움" 실시간 표시         │
                          └────────────────────────────────────────┘
```

## 2. 휴리스틱 + 검증된 근거 기반 분류 알고리즘

| 단계 | 입력 | 계산 | 근거 |
|------|------|------|------|
|① 중력 벡터 추출|ACC|`pitch = asin(-ax/g)`<br>`roll = atan2(ay, az)`|Shoaib 2016, Felton 2020 (90 %↑)|
|② 자세 후보 결정|`pitch, roll`|`lying if |pitch| or |roll| > 50°`|손목이 수평에 가까우면 눕기|
|③ 고도 기반 보정|BARO (hPa)|Kalman / LPF → Δh(m)|Δh 30–50 cm로 앉/서 구분 – Massé 2014 (99.5 %)|
|④ 최종 분류|후보, Δh|`standing if Δh ≥ 0.4 m`<br>`sitting if Δh ≤ 0.25 m`|Park 2019 RF 94 %|
|⑤ 지터 완화|1.5 s(6 샘플) majority vote|—|Shoaib 2016|

> **왜 ML이 아닌 휴리스틱인가?**  
> • Δh + 기울기만으로 SVM/RF에 필적(∼94 %)  
> • 250 ms / 25 Hz 전송으로 지연 < 120 ms  
> • TFLite >300 kB 모델은 전송량·배터리 부담 ↑

## 3. 주요 코드 스니펫

```kotlin
// mobile/src/…/PostureClassifier.kt – 일부 발췌
val candidate = when {
    (abs(pitch) > 50 || abs(roll) > 50) && deltaH < 0.15 -> Posture.LYING
    deltaH >= 0.40 && abs(pitch) < 35 && abs(roll) < 35 -> Posture.STANDING
    deltaH <= 0.25 -> Posture.SITTING
    else -> prevPosture
}
```

```kotlin
// wear/src/…/ProtoWearSensorService.kt – 패킷 전송부
ByteBuffer.allocate(8*8)
    .putDouble(timestamp.toDouble())
    .putDouble(ax.toDouble()).putDouble(ay.toDouble()).putDouble(az.toDouble())
    .putDouble(gx.toDouble()).putDouble(gy.toDouble()).putDouble(gz.toDouble())
    .putDouble(pressure.toDouble())
    .array()
```

## 4. 폴더 / 모듈 구조

```
ProtoType/
├─ mobile/        # Android Phone 앱 (UI · 분류 로직)
│  └─ src/main/java/com/example/prototype/
│      ├─ PostureClassifier.kt
│      └─ ProtoBleReceiverService.kt
├─ wear/          # Wear OS 앱 (센서 수집 · BLE 전송)
│  └─ src/main/java/com/example/prototype/
│      └─ ProtoWearSensorService.kt
├─ build.gradle.kts (root)
└─ settings.gradle.kts (모듈 선언)
```

## 5. 개발 환경 & 빌드 방법

1. **사전 요구**  
   • Android Studio Meerkat 이상  
   • Galaxy Watch 7 (Wear OS 4) + Galaxy S22 (Android 15) 페어링  
   • JDK 17, Kotlin 1.9.x
2. 프로젝트 열기 → *Phone & Wear Module* 템플릿으로 생성한 작업 공간에 본 소스 덮어쓰기
3. Android 15 에뮬레이터 대신 실기기 연결 권장 (BLE Data-Layer 측정)
4. `Run ▶ wear` → 워치 앱 설치 후 **Calibrate** 버튼으로 기준 고도 저장
5. `Run ▶ mobile` → 휴대폰 앱 실행, UI에 자세 업데이트 확인

## 6. 사용 방법

| 동작 | 설명 |
|------|------|
|Calibrate|서있는 상태에서 터치 → 기준 기압(고도)을 캡처|
|Sit / Stand / Lie|손목 자세·고도 변화 실험 → UI 텍스트가 실시간 갱신|

## 7. 테스트 시나리오 권장

- 10 명 × 일상(걷기·타이핑·의자 회전) 시나리오 로깅
- BLE 패킷 드롭률과 분류 Confusion Matrix 분석

## 8. Roadmap

| 단계 | 내용 | 목표 |
|------|------|------|
|① 성능 검증|실사용 데이터셋 수집·분석|>90 % F1|
|② ML 모델화|25 Hz·2 s 윈도우 특징 → Random Forest|Latency < 50 ms|
|③ TFLite 도입|8-bit 양자화 → on-device 추론|Latency < 20 ms|
