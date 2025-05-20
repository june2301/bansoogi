#!/usr/bin/env python
# train_ppg_model_with_calib.py
# pip install numpy pandas scipy scikit-learn tensorflow==2.15.0

import json
import os
import pathlib
import warnings

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
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
mu_raw, sigma_raw = None, None

# ────────────────── 헬퍼 함수 ───────────────────
def detrend(x: np.ndarray) -> np.ndarray:
    """선형 추세 제거 (OLS 공분산/분산 수식으로 Python↔Kotlin 완전 일치)"""
    n = x.size
    idx = np.arange(n, dtype=float)

    # 1) 평균 계산
    x_bar = idx.mean()
    y_bar = x.mean()

    # 2) 공분산 및 분산 계산
    cov = np.sum((idx - x_bar) * (x - y_bar))
    var = np.sum((idx - x_bar)**2)

    # 3) slope/intercept
    slope = cov / var if var > 0 else 0.0
    intercept = y_bar - slope * x_bar

    # 4) 트렌드 제거
    trend = slope * idx + intercept
    return x - trend


def bandpass(x: np.ndarray, lo=0.5, hi=5.0, fs=FS, order=2) -> np.ndarray:
    """0.5–5Hz 2차 Butterworth zero‑phase 필터"""
    nyq = 0.5 * fs
    b, a = butter(order, [lo/nyq, hi/nyq], btype='band')
    y = filtfilt(b, a, x)
    return filtfilt(b, a, y)


def extract_features(seg: np.ndarray) -> np.ndarray:
    """10초 세그먼트에서 HRV‑10 피처 계산
       (1) detrend → (2) z‑score(normalize) → (3) band‑pass filter → (4) feature"""
    # 1) 선형 추세 제거
    detrended = detrend(seg)
    # 2) Z‑score normalization (raw 그린 채널 μ/σ 로딩해서 전역에 저장한 mu_raw, sigma_raw 사용)
    #    (Kotlin 쪽과 동일하게 filter 전에 normalize)
    if sigma_raw > 0:
        normed = (detrended - mu_raw) / sigma_raw
    else:
        normed = detrended - mu_raw
    # 3) band‑pass filter
    x = bandpass(normed)
    # peak/trough (min distance = FS*0.4 샘플)
    min_dist = int(FS * 0.4)
    peaks,   _ = find_peaks(  x, distance=min_dist)
    troughs, _ = find_peaks(- x, distance=min_dist)
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
    # float32 로 내리기 (Kotlin Float32 오차와 매칭)
    return feats.astype(np.float32)


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
    # 피처 컬럼을 모두 float32 로 강제 변환
    df[FEATURE_NAMES] = df[FEATURE_NAMES].astype(np.float32)
    return df.dropna()

# ─────────────────── 메인 ───────────────────
if __name__ == '__main__':
    # 1) 기존 raw stats 로드 (z-score normalization 용)
    calib_file = pathlib.Path('models/train_calib.json')
    # main() 시작 부분에서
    with open(calib_file, encoding='utf-8') as f:
        orig = json.load(f)
    mu_raw    = orig['stats_raw']['green']['mu']
    sigma_raw = orig['stats_raw']['green']['sigma']

    # 2) 데이터 준비
    df = load_data(mu_raw, sigma_raw)
    print('Dataset windows:', df.shape)

    # 3) 글로벌 feature calibration 재계산 (10-90% 클램핑 + quantile bins)
    def compute_global_calib(df, low_clip=0.10, high_clip=0.90, bins=10):
        calib = {
            'stats': {},
            'bin_edges': {},
            'bin_edges_z': {},
            'clip_bounds': {}    # 여기에 clip bounds 저장
        }
        for f in FEATURE_NAMES:
            s = df[f].dropna()
            # 1) quantile 기반 하한/상한 계산
            lo = s.quantile(low_clip)
            hi = s.quantile(high_clip)

            # 2) 클램핑 적용
            sc = s.clip(lo, hi)

            # 3) stats, bin_edges 계산
            mu = float(sc.mean())
            sigma = float(sc.std(ddof=0))
            calib['stats'][f] = {'mu': mu, 'sigma': sigma}

            qs = np.linspace(0, 1, bins + 1)
            calib['bin_edges'][f]   = sc.quantile(qs).values.tolist()
            calib['bin_edges_z'][f] = np.linspace(-3, 3, bins + 1).tolist()

            # 4) clip_bounds 기록
            calib['clip_bounds'][f] = [float(lo), float(hi)]

        # 기존 raw stats, calib_means 등이 있다면 그대로 병합
        calib['stats_raw'] = orig['stats_raw']
        # 예: calib['calib_means'] = orig['calib_means']

        return calib

    new_calib = compute_global_calib(df)
    # 기본 subjectMeans 를 global feature means 로 채워 넣기
    new_calib['calib_means'] = {
        f: new_calib['stats'][f]['mu']
        for f in FEATURE_NAMES
    }
    # JSON overwrite
    with open(calib_file, 'w', encoding='utf-8') as f:
        json.dump(new_calib, f, indent=2)
    print('Updated calibration JSON with clamped quantile bins.')

    # 4) feature normalization using updated calibration
    mu_arr    = np.array([new_calib['stats'][f]['mu']    for f in FEATURE_NAMES], dtype=np.float32)
    sigma_arr = np.array([new_calib['stats'][f]['sigma'] for f in FEATURE_NAMES], dtype=np.float32)
    X = ((df[FEATURE_NAMES].values.astype(np.float32) - mu_arr) / sigma_arr).astype(np.float32)

    # --- enforce y as integer array ---
    label_map = { l:i for i,l in enumerate(LABELS) }
    df['label'] = df['label'].astype(str).map(label_map)  # ✅ 문자열 라벨을 명시적으로 str로 변환 후 매핑
    if df['label'].isnull().any():
        raise ValueError("Some labels could not be mapped to integers. Check LABELS and input data.")
    y = df['label'].astype(np.int32).to_numpy()

    # 🔒 그룹도 LabelEncoder로 정수로 변환하여 Keras dtype promotion 문제 해결
    from sklearn.preprocessing import LabelEncoder
    df['subject'] = df['subject'].astype(str)
    g = LabelEncoder().fit_transform(df['subject'])

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
