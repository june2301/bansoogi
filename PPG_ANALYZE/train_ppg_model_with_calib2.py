#!/usr/bin/env python
# train_ppg_model_with_calib.py – 완전 수정본
# -------------------------------------------
#   · dtype='string' 오류 방지 (모든 배열을 float32 / int32 로 강제)
#   · 기존 HRV‑10 전처리 로직 그대로 유지
#   · 녹화된 **원시 green 신호** 전체에서 μ/σ 재계산하여 calib.json 의
#       stats_raw.green.mu / sigma 를 자동 갱신
#   · Leave‑One‑Subject‑Out(LOSO) 교차검증 + 최종 TFLite 저장
# --------------------------------------------------------------
# pip install numpy pandas scipy scikit-learn tensorflow==2.15.0

import json
import pathlib
import warnings

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import LabelEncoder
from tensorflow.keras import layers, models
import tensorflow as tf

warnings.filterwarnings("ignore")

# ─────────────────────────── 설정 ───────────────────────────
BASE_DIR   = pathlib.Path("recordings")   # JSON recordings 폴더 루트
WIN_S      = 10                            # 윈도우 길이 (초)
OVL        = 0.30                          # 윈도우 오버랩 비율 (30 %)
FS         = 25                            # 샘플링 주파수 (Hz)
LABELS     = ['upright-sitting', 'supine-lying', 'standing']
FEATURES   = [  # HRV‑10
    'pnn50','rr_mean','hr_mean','rmssd','n_peaks',
    'crest_t','dwell_t','pwtf','kurtosis','skew'
]

# ────────────────── 신호 전처리 헬퍼 ──────────────────

def detrend(x: np.ndarray) -> np.ndarray:
    n   = x.size
    idx = np.arange(n, dtype=float)
    coef = np.polyfit(idx, x, 1)  # 1차 회귀
    return x - np.polyval(coef, idx)


def bandpass(x: np.ndarray, lo=0.5, hi=5.0, fs=FS, order=2) -> np.ndarray:
    nyq = 0.5 * fs
    b, a = butter(order, [lo/nyq, hi/nyq], btype='band')
    return filtfilt(b, a, filtfilt(b, a, x))  # zero‑phase 4‑pole


def extract_features(seg: np.ndarray, mu_raw: float, sigma_raw: float) -> np.ndarray:
    """HRV‑10 피처 (train 파이프라인과 동일)"""
    x = bandpass((detrend(seg) - mu_raw) / sigma_raw)

    min_dist = int(FS * 0.4)
    peaks, _   = find_peaks( x, distance=min_dist)
    troughs, _ = find_peaks(-x, distance=min_dist)
    rr = np.diff(peaks) / FS if peaks.size > 1 else np.array([])

    f = np.zeros(len(FEATURES), dtype=np.float32)
    f[FEATURES.index('n_peaks')] = peaks.size

    if rr.size:
        rr_mean = rr.mean(); f[FEATURES.index('rr_mean')] = rr_mean
        f[FEATURES.index('hr_mean')] = 60./rr_mean
    if rr.size > 1:
        diff = np.diff(rr)
        f[FEATURES.index('rmssd')] = np.sqrt(np.mean(diff**2))
        f[FEATURES.index('pnn50')] = np.mean(np.abs(diff) > 0.05)

    # crest / dwell / pwtf
    triples = []
    for p in peaks:
        left  = troughs[troughs < p]
        right = troughs[troughs > p]
        if left.size and right.size:
            triples.append((left.max(), p, right.min()))
    if triples:
        ct = np.mean([(p-f)/FS for f,p,_ in triples])
        dw = np.mean([(n-f)/FS for f,_,n in triples])
        f[FEATURES.index('crest_t')] = ct
        f[FEATURES.index('dwell_t')] = dw
        f[FEATURES.index('pwtf')] = ct / dw if dw else 0.

    # kurtosis / skewness
    f[FEATURES.index('kurtosis')] = pd.Series(x).kurtosis()
    f[FEATURES.index('skew')]     = pd.Series(x).skew()

    return f


def sliding_windows(sig: np.ndarray):
    length = WIN_S * FS
    step   = int(length * (1-OVL))
    for s in range(0, len(sig)-length+1, step):
        yield sig[s:s+length]

# ─────────────────── 데이터 로딩 ───────────────────

def load_dataset(mu_raw: float, sigma_raw: float):
    rows, labels, subjects = [], [], []
    raw_concat = []  # green 원본 μ/σ 계산용

    for subj in sorted(BASE_DIR.iterdir()):
        if not subj.is_dir():
            continue
        for fn in sorted(subj.glob('*.json')):
            d = json.loads(fn.read_text(encoding='utf-8'))
            label = d['label'].split('/')[-1].strip()
            ts    = np.array(d['data']['ppg_continuous']['ts'], dtype=float)
            raw   = np.array(d['data']['ppg_continuous']['green'], dtype=float) / 4096.0
            raw_concat.append(raw)
            sig = raw[ts >= ts[0] + 5000]           # 5 초 워밍업 컷
            for seg in sliding_windows(sig):
                rows.append(extract_features(seg, mu_raw, sigma_raw))
                labels.append(label)
                subjects.append(subj.name)

    df = pd.DataFrame(rows, columns=FEATURES)
    df['label']   = labels
    df['subject'] = subjects
    return df, np.concatenate(raw_concat)

# ─────────────────── 메인 ───────────────────

if __name__ == '__main__':
    calib_fp = pathlib.Path('models/train_calib.json')
    calib_fp.parent.mkdir(parents=True, exist_ok=True)

    # -------- 0) 기존 calib 파일 로드 (없으면 기본값) --------
    if calib_fp.exists() and calib_fp.stat().st_size:
        calib = json.loads(calib_fp.read_text(encoding='utf-8'))
    else:
        calib = {
            'stats_raw': {'green': {'mu': 0.0, 'sigma': 1.0}},
            'stats': {}, 'clip_bounds': {}, 'calib_means': {}
        }

    mu_raw    = float(calib['stats_raw']['green']['mu'])
    sigma_raw = float(calib['stats_raw']['green']['sigma'])

    # -------- 1) 데이터 로딩 + feature 추출 --------
    df, raw_all = load_dataset(mu_raw, sigma_raw)
    print('Dataset windows:', df.shape)

    # -------- 2) raw green μ/σ 재계산 & 업데이트 --------
    mu_raw_new    = float(raw_all.mean())
    sigma_raw_new = float(raw_all.std(ddof=0)) if raw_all.size else 1.0
    calib['stats_raw'] = {'green': {'mu': mu_raw_new, 'sigma': sigma_raw_new}}

    # -------- 3) 글로벌 feature 통계 재계산 (10‑90% 클램핑) --------
    stats = {}; clip = {}
    for f in FEATURES:
        s = df[f].astype(float).dropna()
        lo, hi = s.quantile(0.10), s.quantile(0.90)
        sc = s.clip(lo, hi)
        stats[f] = {'mu': float(sc.mean()), 'sigma': float(sc.std(ddof=0))}
        clip[f]  = [float(lo), float(hi)]
    calib['stats'] = stats
    calib['clip_bounds'] = clip
    calib['calib_means'] = {f: stats[f]['mu'] for f in FEATURES}

    # -------- 4) calib.json 저장 --------
    calib_fp.write_text(json.dumps(calib, indent=2))
    print('calib.json updated (stats_raw + feature stats).')

    # -------- 5) feature 정규화 --------
    mu_vec = np.array([stats[f]['mu'] for f in FEATURES], dtype=np.float32)
    sd_vec = np.array([stats[f]['sigma'] for f in FEATURES], dtype=np.float32)
    X = ((df[FEATURES].values.astype(np.float32) - mu_vec) / sd_vec).astype(np.float32)

    # 라벨/그룹 정수 인코딩 (dtype 오류 방지) --------
    y = df['label'].map({l: i for i, l in enumerate(LABELS)}).astype(np.int32).to_numpy()
    g = LabelEncoder().fit_transform(df['subject'].astype(str))

    # -------- 6) MLP + LOSO‑CV --------
    logo = LeaveOneGroupOut()
    accs = []
    for tr, te in logo.split(X, y, g):
        model = models.Sequential([
            layers.Input(shape=(len(FEATURES),)),
            layers.Dense(128, activation='relu'), layers.Dropout(0.3),
            layers.Dense(64, activation='relu'),  layers.Dropout(0.3),
            layers.Dense(3, activation='softmax')
        ])
        model.compile('adam', 'sparse_categorical_crossentropy', ['accuracy'])
        model.fit(X[tr], y[tr], epochs=40, batch_size=32, verbose=0)
        accs.append(model.evaluate(X[te], y[te], verbose=0)[1])
    print(f'MLP LOSO‑CV: {np.mean(accs):.3f} ± {np.std(accs):.3f}')

    # -------- 7) 전체 데이터로 재학습 & TFLite 저장 --------
    final = models.clone_model(model)
    final.compile('adam', 'sparse_categorical_crossentropy', ['accuracy'])
    final.fit(X, y, epochs=40, batch_size=32, verbose=0)

    out_dir = pathlib.Path('models'); out_dir.mkdir(exist_ok=True)
    final.save(out_dir / 'ppg_10s_30.keras')

    # TFLite 변환 및 저장
    tflite_model = tf.lite.TFLiteConverter.from_keras_model(final).convert()
    (out_dir / 'ppg_10s_30.tflite').write_bytes(tflite_model)
    print('Final models saved: .keras and .tflite files in', out_dir)
