# -*- coding: utf-8 -*-
"""공통 설정"""
FS = 25.0                                # Hz
WIN_RAW_SEC  = 5                         # 5 s  (raw Conv)
WIN_FEAT_SEC = 15                        # 15 s (feature)
OVERLAP_RATE = 0.50                      # 70 %
BAND_RAW   = (0.5, 8.0)
BAND_FEAT  = (0.05, 4.0)
SQI_THRESH = -5.0                        # dB
# Training
BATCH_SIZE = 64
EPOCHS     = 80
INIT_LR    = 1e-4
PATIENCE   = 10
DEBUG = False
