#!/usr/bin/env python
# train_ppg_model_with_calib.py
# pip install numpy pandas scipy scikit-learn tensorflow==2.15.0

import json
import os
import pathlib
import warnings

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt
from sklearn.model_selection import LeaveOneGroupOut
from tensorflow.keras import layers, models
import tensorflow as tf

warnings.filterwarnings("ignore")

# ─────────────────────────────── 설정 ───────────────────────────────
BASE_DIR = pathlib.Path("recordings")        # JSON recordings 폴더
WIN_S = 10                                   # 윈도우 길이(초)
OVL = 0.0                                    # 30% 오버랩
FS = 25                                      # 샘플링 주파수
LABELS = ['upright-sitting', 'supine-lying', 'standing']
FEATURE_NAMES = [
    'pnn50','rr_mean','hr_mean','rmssd','n_peaks',
    'crest_t','dwell_t','pwtf','kurtosis','skew'
]

# ────────────────── 헬퍼 함수 ───────────────────
def detrend(x: np.ndarray) -> np.ndarray:
    """선형 추세 제거"""
    n = x.size
    idx = np.arange(n)
    coef = np.polyfit(idx, x, 1)
    trend = np.polyval(coef, idx)
    return x - trend


def bandpass(x: np.ndarray, lo=0.5, hi=5.0, fs=FS, order=2) -> np.ndarray:
    """0.5–5Hz 2차 Butterworth zero‑phase 필터"""
    nyq = 0.5 * fs
    b, a = butter(order, [lo/nyq, hi/nyq], btype='band')
    y = filtfilt(b, a, x)
    return filtfilt(b, a, y)


def extract_features(seg: np.ndarray) -> np.ndarray:
    """10초 세그먼트에서 HRV‑10 피처 계산"""
    # detrend + bandpass
    x = bandpass(detrend(seg))
    # peak/trough
    peaks = np.where((x[1:-1] > x[:-2]) & (x[1:-1] > x[2:]))[0] + 1
    troughs = np.where((x[1:-1] < x[:-2]) & (x[1:-1] < x[2:]))[0] + 1
    rr = np.diff(peaks) / FS if peaks.size > 1 else np.array([])

    feats = np.zeros(len(FEATURE_NAMES), dtype=float)
    # n_peaks, rr_mean, hr_mean
    feats[FEATURE_NAMES.index('n_peaks')] = peaks.size
    if rr.size:
        feats[FEATURE_NAMES.index('rr_mean')] = rr.mean()
        feats[FEATURE_NAMES.index('hr_mean')] = 60.0 / rr.mean()
    # rmssd, pnn50
    if rr.size > 1:
        diffs = np.diff(rr)
        feats[FEATURE_NAMES.index('rmssd')] = np.sqrt(np.mean(diffs**2))
        feats[FEATURE_NAMES.index('pnn50')] = np.mean(np.abs(diffs) > 0.05)
    # crest_t, dwell_t, pwtf
    ct_vals, dw_vals = [], []
    for p in peaks:
        prevs = troughs[troughs < p]
        nexts = troughs[troughs > p]
        if prevs.size and nexts.size:
            ct_vals.append((p - prevs.max()) / FS)
            dw_vals.append((nexts.min() - prevs.max()) / FS)
    if ct_vals:
        feats[FEATURE_NAMES.index('crest_t')] = np.mean(ct_vals)
        feats[FEATURE_NAMES.index('dwell_t')] = np.mean(dw_vals)
        feats[FEATURE_NAMES.index('pwtf')] = np.mean(ct_vals) / np.mean(dw_vals)
    # kurtosis, skewness
    feats[FEATURE_NAMES.index('kurtosis')] = pd.Series(x).kurtosis()
    feats[FEATURE_NAMES.index('skew')] = pd.Series(x).skew()
    return feats


def sliding_windows(sig: np.ndarray) -> np.ndarray:
    step = int(WIN_S * FS * (1 - OVL))
    length = WIN_S * FS
    for start in range(0, sig.size - length + 1, step):
        yield sig[start:start + length]

# ─────────────────── 데이터 로딩 ───────────────────
def load_data(mu: float, sigma: float) -> pd.DataFrame:
    rows, labels, subjects = [], [], []
    for subj_dir in sorted(BASE_DIR.iterdir()):
        if not subj_dir.is_dir(): continue
        for fn in sorted(subj_dir.glob('*.json')):
            d = json.loads(fn.read_text(encoding='utf-8'))
            label = d['label'].split('/')[-1].strip()
            ts = np.array(d['data']['ppg_continuous']['ts'], dtype=float)
            sig = np.array(d['data']['ppg_continuous']['green'], dtype=float) / 4096.0
            sig = sig[ts >= ts[0] + 5000]  # 5초 warm-up
            for seg in sliding_windows(sig):
                feats_temp = extract_features(seg)
                hr_idx = FEATURE_NAMES.index('hr_mean')
                hr_val = feats_temp[hr_idx]
                feats = feats_temp
                feats = extract_features(seg)
                rows.append(feats)
                labels.append(label)
                subjects.append(subj_dir.name)
    df = pd.DataFrame(rows, columns=FEATURE_NAMES)
    df['label'], df['subject'] = labels, subjects
    return df.dropna()

# ─────────────────── 메인 ───────────────────
if __name__ == '__main__':
    # 1) 기존 raw stats 로드 (z-score normalization 용)
    calib_file = pathlib.Path('models/train_calib.json')
    with open(calib_file, encoding='utf-8') as f:
        orig = json.load(f)
    mu_raw = orig['stats_raw']['green']['mu']
    sigma_raw = orig['stats_raw']['green']['sigma']

    # 2) 데이터 준비
    df = load_data(mu_raw, sigma_raw)
    print('Dataset windows:', df.shape)

    # 3) 글로벌 feature calibration 재계산 (10-90% 클램핑 + quantile bins)
    def compute_global_calib(df, low_clip=0.10, high_clip=0.90, bins=10):
        calib = {'stats': {}, 'bin_edges': {}, 'bin_edges_z': {}}
        for f in FEATURE_NAMES:
            s = df[f].dropna()
            lo = s.quantile(low_clip); hi = s.quantile(high_clip)
            sc = s.clip(lo, hi)
            mu = float(sc.mean()); sigma = float(sc.std(ddof=0))
            calib['stats'][f] = {'mu': mu, 'sigma': sigma}
            qs = np.linspace(0,1,bins+1)
            calib['bin_edges'][f]   = sc.quantile(qs).values.tolist()
            calib['bin_edges_z'][f] = np.linspace(-3,3,bins+1).tolist()
        calib['stats_raw'] = orig['stats_raw']
        return calib

    new_calib = compute_global_calib(df)
    # JSON overwrite
    with open(calib_file, 'w', encoding='utf-8') as f:
        json.dump(new_calib, f, indent=2)
    print('Updated calibration JSON with clamped quantile bins.')

    # 4) feature normalization using updated calibration
    mu_arr = np.array([new_calib['stats'][f]['mu'] for f in FEATURE_NAMES])
    sigma_arr = np.array([new_calib['stats'][f]['sigma'] for f in FEATURE_NAMES])
    X = (df[FEATURE_NAMES].values - mu_arr) / sigma_arr
    y = df['label'].map({l:i for i,l in enumerate(LABELS)}).values
    g = df['subject'].values

    # 5) MLP 모델 정의 & LOSO-CV
    logo = LeaveOneGroupOut()
    accs = []
    for tr, te in logo.split(X, y, g):
        model = models.Sequential([
            layers.Input(shape=(len(FEATURE_NAMES),)),
            layers.Dense(128, activation='relu'), layers.Dropout(0.3),
            layers.Dense(64, activation='relu'),  layers.Dropout(0.3),
            layers.Dense(3, activation='softmax')
        ])
        model.compile('adam', 'sparse_categorical_crossentropy', ['accuracy'])
        model.fit(X[tr], y[tr], epochs=50, batch_size=32, verbose=0)
        accs.append(model.evaluate(X[te], y[te], verbose=0)[1])
    print(f'MLP LOSO-CV: {np.mean(accs):.3f} ± {np.std(accs):.3f}')

    # 6) 전체 학습 후 TFLite 저장
    final = models.clone_model(model)
    final.compile('adam', 'sparse_categorical_crossentropy', ['accuracy'])
    final.fit(X, y, epochs=50, batch_size=32, verbose=0)
    out_dir = pathlib.Path('models'); out_dir.mkdir(exist_ok=True)
    keras_path = out_dir / 'ppg_10s_30.keras'
    final.save(keras_path)
    tflite = tf.lite.TFLiteConverter.from_keras_model(final).convert()
    (out_dir / 'ppg_10s_30.tflite').write_bytes(tflite)
    print('Models and TFLite saved.')
