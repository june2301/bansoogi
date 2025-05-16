"""공통 설정 및 증강 파라미터"""

FS = 25.0                       # Hz
WIN_RAW_SEC  = 5                # 5s raw window
WIN_FEAT_SEC = 15               # 15s feature window
OVERLAP_RATE = 0.50             # 50% base overlap

# Jitter: 오버랩 사이즈의 절반을 최대 이동 거리로 사용
STEP_FEAT = int(WIN_FEAT_SEC * FS * (1 - OVERLAP_RATE))
JITTER_MAX = STEP_FEAT // 2     # ±샘플

# Band-pass
BAND_RAW  = (0.5, 8.0)
BAND_FEAT = (0.05, 4.0)
SQI_THRESH = -5.0               # dB

# Augmentation
AUG_GAUSS_STD = 0.005           # Gaussian noise σ
AUG_MAG_SCALE = 0.05            # ±5% magnitude scale
AUG_REPEAT    = 2               # 윈도우당 증강 횟수

# Training
BATCH_SIZE = 64
EPOCHS     = 80
INIT_LR    = 1e-4
PATIENCE   = 10

DEBUG = False