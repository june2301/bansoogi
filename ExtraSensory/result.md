# Galaxy Watch HAR 파이프라인 (standing / sitting / lying)

> **리비전 v1 (2025-05-07)**  
> Galaxy Watch 7 (40 mm) 단일 손목 IMU만으로 자유환경(in-the-wild)에서 “서기(standing) / 앉기(sitting) / 눕기(lying)” 3가지 정적 자세를 분류하는 경량 파이프라인

---

## 🚀 1. 실행 계획

### 1.1 목표

- **정적 자세 분류**: 자유생활 환경에서 손목 IMU(acc + gyro)로 “서기 / 앉기 / 눕기” 구분
- **동적·특수 활동 필터링**: 걷기·뛰기·계단·운동·수면은 OS 수준 (Wear OS Health Services / SensorManager) 에서 제외

### 1.2 데이터셋 및 라벨 매핑

| 소스             | 설명                            | 라벨 매핑                                                 |
| ---------------- | ------------------------------- | --------------------------------------------------------- |
| **ExtraSensory** | 60명·최대 7일·손목 IMU 데이터   | `standing→2`, `lying_down→1`, `sitting→0`                 |
| **CAPTURE-24**   | 151명·24 h·손목 IMU + 수면 기록 | `sleep*→1`, `sitting*→0`, **standing 라벨 없음 → 미사용** |

→ 클래스 불균형 대응: `class_weight = {0:1.5, 1:0.5, 2:2.0}` 권장

### 1.3 전처리 파이프라인

| 단계              | 설정 / 값                             | 근거 및 비고                              |
| ----------------- | ------------------------------------- | ----------------------------------------- |
| **샘플링 주파수** | 25 Hz                                 | 50 Hz 대비 배터리 소모 절반               |
| **저역통과 필터** | 4극 버터워스, 12 Hz                   | Nyquist 상한의 90 %                       |
| **윈도잉**        | 5 s (125샘플), 50 % 중첩              | 교차 검증 F₁ 최적                         |
| **Dynamic-Skip**  | SMA ≥ 0.30 g OR GyroRMS ≥ 0.05 rad/s  | 미세 동적 움직임 윈도우 배제 → 호출 70 %↓ |
| **단위 변환**     | ExS: mg→m/s², CAP: g→m/s²             | 통일된 물리 단위                          |
| **라벨 조회**     | 윈도우 시작 타임스탬프 기준 dict 조회 | ExtraSensory 라벨 타임스탬프 정밀 매핑    |
| **히스테리시스**  | 연속 2개 윈도우(\~7.5 s) 동일 시 확정 | 노이즈·잔류 떨림 억제                     |

### 1.4 모델 아키텍처 및 하이퍼파라미터

```

Input: 125×3
→ Conv1D(32, kernel=5) → ReLU
→ Conv1D(64, kernel=5) → ReLU
→ GlobalAveragePooling1D
→ Dense(3, activation='softmax')  # standing/sitting/lying

```

- 파라미터 수: 46 112개 (≈155 KB fp32)
- Optimizer/Epochs/Batch: Adam / 25 / 64
- Train/Val Split: 80 % / 20 % (랜덤)
- Export: `model/sitlie_v1.h5` → `sitlie_v1.tflite`

### 1.5 코드 구조 & 빠른 실행

```

ExtraSensory/
├─ data/
│  ├─ exs\_raw/   # ExtraSensory .dat
│  ├─ cap\_raw/   # CAPTURE-24 .csv
│  └─ npz/       # 생성된 windows npz
├─ model/       # .h5, .tflite
├─ logs/        # TensorBoard 로그
└─ src/
└─ watch\_har\_pipeline.py

```

```bash
# 1) NPZ 생성
python src/watch_har_pipeline.py --prepare

# 2) 모델 학습 & TFLite 변환
python src/watch_har_pipeline.py --train
```

---

## 📝 2. 실행 결과

### 2.1 데이터 통계

- **윈도우 개수**

  - ExtraSensory → 112 468개
  - CAPTURE-24 → 2 092 074개

### 2.2 학습 로그\_v1

| 지표           | 최고 에포크 | 값                       |
| -------------- | ----------- | ------------------------ |
| **Val 정확도** | 23          | **0.8209**               |
| Val 손실       | 23          | 0.4377                   |
| 학습 시간      | —           | 약 1시간 45분 (CPU-only) |

<details>
<summary>에포크별 상세</summary>
```
Epoch  1: loss=0.5394, acc=0.7812 → val_acc=0.7985  
…  
Epoch 25: loss=0.4393, acc=0.8198 → val_acc=0.8209
```
</details>

### 2.3 배터리 & 저장 효율

- **추론 호출**: Dynamic-Skip 적용 시 호출 30 %
- **배터리 소모**: 25 Hz + Dynamic-Skip → Watch 7 기준 시간당 < 4 mAh
- **저장**: 15 분당 270 B → 일일 < 0.8 MB

---

## 🔍 3. 결과 분석

### 3.1 주요 실패 원인

1. **절대 자세 정보 부재**

   - 손목 IMU는 상대 가속도·회전 속도만 측정 → 몸통 기울기·고관절 각도 불검출

2. **높은 클래스 내 변동성**

   - 팔 동작(타이핑·지지대) 다양 → 동일 자세라 해도 센서 신호 크게 달라짐

3. **판별 피처 부재**

   - 앉음/서기/눕기에 고유한 일관된 피처(고관절 각도, 체중 분포 등) 미측정

4. **엣지 케이스 빈번**

   - 테이블·소파 팔걸이 등 정적 자세 간 센서 패턴 겹침 → 오분류 불가피

5. **정보론적 한계**

   - 결정적 피처가 없으면 전처리·모델링 기법 모두 “Garbage In → Garbage Out”

### 3.2 이론적 정리

- **분류 성공 요건**:

  1. Discriminative feature 존재
  2. Low intra-class variance
  3. High inter-class variance

- 손목 IMU 신호는 위 조건 모두 만족하지 못해 **본질적 분류 한계**에 직면

### 3.3 제언 및 다음 단계

1. **추가 센서 도입**: 척추 경사·발 체중 등 절대 자세 정보 확보
2. **멀티모달 융합**: IMU + PPG/심박수/카메라 결합
3. **Semi-supervised 개인화**: 사용자별 초기 캘리브레이션
4. **모델 경량화 & 양자화**: INT8 → 크기 \~40 KB, F₁ >= 0.90 목표
5. **필드 데이터 수집**: 실제 사용자 환경에서 보정 단계 도입

---

> _Maintainer_: Kee-Hoon Won | PR/이슈 언제든 환영
