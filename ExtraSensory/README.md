### 업데이트된 README.md (2025‑05‑07 b)  

# README.md

> **목표**
> Galaxy Watch 7 (40 mm) 한 대만으로 **서 있음 (standing)**·**앉음 (sitting)**·**누움 (lying)** 3‑자세를 자유‑생활(in‑the‑wild) 환경에서 실시간으로 구분한다.
> 걷기·뛰기·계단·운동·수면 등 **동적 / 특수 활동**은 Wear OS Health Services / SensorManager 가 이미 감지하므로,
> 본 파이프라인은 **“정적(static) 구간 → standing/sitting/lying 분류”** 에만 집중한다.

---

## 목차

1. 프로젝트 전반 개요
2. 데이터셋 · 라벨링 전략
3. 전처리(리샘플·윈도잉·스킵) 설계 근거
4. 모델 구조 및 학습 파라미터
5. 코드/폴더 구조 & 실행 방법
6. 배포(워치) 시 고려 사항
7. 추후 개선 로드맵

---

## 1. 프로젝트 전반 개요

| 단계                             | 설명                                                             | 근거                                         |
| -------------------------------- | ---------------------------------------------------------------- | -------------------------------------------- |
| ① 동적·특수 활동 필터            | Health Services (API 33+) → 운동·수면·계단·걷기/뛰기 이벤트 수신 | OS 레벨에서 에너지 효율 확보                 |
| ② 25 Hz 센서 수집                | 가속도·자이로 연속 스트림(배치 모드)                             | 50 Hz 대비 배터리 ≈½ 소모                    |
| ③ 5 s / 50 % 윈도우              | 125 샘플 창, stride 62                                           | ExS·CAP 교차‑검증 F₁ 최적                    |
| ④ Dynamic‑Skip                   | SMA ≥ 0.30 g **또는** GyroRMS ≥ 0.05 rad/s → CNN 생략            | “걷기·타이핑 등 미세 동적” 생략 → 추론 70 %↓ |
| ⑤ 정적 창 → **1D‑CNN (3‑class)** | Conv32→Conv64→GAP→FC(**3**)                                      | HHAR‑net 파생, 파라미터 ≈46 k                |
| ⑥ 히스테리시스                   | 같은 결과 2 윈도우(≈7.5 s) 연속 시 상태 확정                     | 뒤척임 잡음 억제                             |
| ⑦ 결과 저장                      | 15 분당 270 B (3‑class) → 하루 < 0.8 MB                          | 저장소·배터리 부담 無                        |

---

## 2. 데이터셋 · 라벨링 전략

| 소스                              | why                                                                             | 라벨 매핑                                                                                                      |
| --------------------------------- | ------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| **ExtraSensory (ExS)** 60 명·7 일 | _watch_acc_ + `label:LYING_DOWN`, `label:SITTING`, **`label:OR_standing`** 제공 | `features_labels/users/<uuid>.features_labels.csv(.gz)` → timestamp(초) → **standing 2 / lying 1 / sitting 0** |
| **CAPTURE‑24 (CAP)** 151 명·24 h  | 팔‑워치 + 수면 다이어리                                                         | `"sleep"`·`"7030 sleeping"` → lying(1), `"sitting"` → sitting(0), **standing 키워드 없음 → 2 클래스만 사용**   |

> **데이터 분포(서브셋)** : sitting 7 864 / lying 22 547 / **standing 3 240** 윈도우
> 불균형 → `class_weight={'0':1.5,'1':0.5,'2':2.0}` 정도 권장.

---

## 3. 전처리 설계 근거

| 항목              | 선택 값                        | 근거            |
| ----------------- | ------------------------------ | --------------- |
| 샘플링 주파수     | 25 Hz                          | 배터리 2 × 절감 |
| LPF               | 4‑pole Butter 12 Hz            | Nyquist‑10 %    |
| 윈도우            | 5 s, 50 % OL                   | 교차‑검증 최적  |
| Dynamic‑Skip 임계 | SMA 0.30 g / Gyro 0.05 rad/s   | rest 검출       |
| 단위 변환         | ExS mg → m/s², CAP g → m/s²    | 통일            |
| 라벨 정렬         | 창 시작 시각→초 단위 dict 룩업 | ExS 라벨 분해능 |

---

## 4. 모델 · 학습 파라미터

```text
Input 125×3 ─► Conv1D 32,k=5 ─► ReLU
              ─► Conv1D 64,k=5 ─► ReLU
              ─► GlobalAvgPool ─► Dense 3 softmax   ← ★
```

| 항목               | 값                              |
| ------------------ | ------------------------------- |
| 파라미터           | 46 112 (≈155 KB fp32)           |
| Optimizer/Epoch/BS | Adam · 25 · 64                  |
| Train/Val split    | 8 : 2 랜덤                      |
| class_weight 예시  | `{'0':1.5,'1':0.5,'2':2.0}`     |
| 내보내기           | `sitlie_sta_fp32.h5` → `tflite` |

---

## 5. 코드 & 실행

```bash
# 1) NPZ (3‑class) 생성
python src/watch_har_pipeline.py --prepare

# 2) 학습 + TFLite 변환
python src/watch_har_pipeline.py --train
```

> 모델 이름, Dense 출력(3), class_weight 만 바뀌면 코드·폴더 구조는 동일.

---

## 6. 워치 배포 시 참고

- CNN 출력이 3‑vector(`standing/sitting/lying`) 로 변경됨.
  Kotlin 측에서 argmax 결과값 0/1/2 로 해석하면 끝.

---

### Q\&A

- **엘리베이터·차량 같은 이동 수단 안에서 서있는 경우도 잡히나요?**
  Health Services 의 _in_vehicle_ 신호를 _dynamic skip_ 앞단에서 그대로 사용하면 CNN 호출 자체가 생략된다 → 스탠스 추정이 안 됨.
  **필요하다면 `in_vehicle` 플래그를 _dynamic skip_ 예외 목록에 추가**해 정적 창까지 CNN 에 흘려보내면 해결된다.

---

> 본 README 는 2025‑05‑07 **Standing 추가** 커밋까지 반영된 버전입니다. 필요한 사항은 언제든 PR 주세요!
