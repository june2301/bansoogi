# =============================================================================
# configs.py
# =============================================================================

FS = 25.0                 # Hz
WIN_SEC = 15              # 15‑s 윈도우 (375 samples)
OVERLAP = 0.5             # 50 % overlap
BANDPASS = (0.5, 8.0)     # Hz
SQI_THRESH = -5.0         # dB
K_BEST = 8                # top‑K feature selection

# Training
BATCH = 32
EPOCHS = 120
LR = 1e-4
PATIENCE = 10