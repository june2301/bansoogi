# --- Sampling ---
FS = 25.0  # Hz

# --- Window lengths & overlap ---
WIN_RAW_SEC   = 10      # raw Conv 입력용 10 s (250 samples)
WIN_FEAT_SEC  = 15      # 2차 피처용 15 s (375 samples)
OVERLAP_RATE  = 0.50    # 50 %

# --- Band‑pass ---
BANDPASS_RAW   = (0.5, 8.0)   # morphology band
BANDPASS_FEAT  = (0.05, 4.0)  # spectral/HRV band

# --- Signal Quality Index ---
SQI_THRESH = -5.0  # dB, 윈도우 폐기 기준

# --- Training hyper‑params ---
BATCH_SIZE = 32
EPOCHS     = 60
LR         = 1e-4
PATIENCE   = 8

# --- Debug flag ---
DEBUG = False