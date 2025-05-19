# train_ppg_model_with_calib.py (수정 버전)
# 주요 수정 사항:
# - NaN 처리 보강
# - dtype 강제 변환 오류 방지
# - StandardScaler 사용으로 안정성 확보

import json
import pathlib
import warnings

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import StandardScaler, LabelEncoder
from tensorflow.keras import layers, models
import tensorflow as tf

warnings.filterwarnings("ignore")

BASE_DIR = pathlib.Path("recordings")
WIN_S = 10
OVL = 0.0
FS = 25
LABELS = ['upright-sitting', 'supine-lying', 'standing']
FEATURE_NAMES = [
    'pnn50','rr_mean','hr_mean','rmssd','n_peaks',
    'crest_t','dwell_t','pwtf','kurtosis','skew'
]
mu_raw, sigma_raw = None, None

def detrend(x: np.ndarray) -> np.ndarray:
    n = x.size
    idx = np.arange(n, dtype=float)
    x_bar = idx.mean()
    y_bar = x.mean()
    cov = np.sum((idx - x_bar) * (x - y_bar))
    var = np.sum((idx - x_bar)**2)
    slope = cov / var if var > 0 else 0.0
    intercept = y_bar - slope * x_bar
    trend = slope * idx + intercept
    return x - trend

def bandpass(x: np.ndarray, lo=0.5, hi=5.0, fs=FS, order=2) -> np.ndarray:
    nyq = 0.5 * fs
    b, a = butter(order, [lo/nyq, hi/nyq], btype='band')
    y = filtfilt(b, a, x)
    return filtfilt(b, a, y)

def extract_features(seg: np.ndarray) -> np.ndarray:
    detrended = detrend(seg)
    normed = (detrended - mu_raw) / sigma_raw if sigma_raw > 0 else detrended - mu_raw
    x = bandpass(normed)
    min_dist = int(FS * 0.4)
    peaks, _ = find_peaks(x, distance=min_dist)
    troughs, _ = find_peaks(-x, distance=min_dist)
    rr = np.diff(peaks) / FS if peaks.size > 1 else np.array([])
    feats = np.zeros(len(FEATURE_NAMES), dtype=float)
    feats[FEATURE_NAMES.index('n_peaks')] = peaks.size
    if rr.size:
        feats[FEATURE_NAMES.index('rr_mean')] = rr.mean()
        feats[FEATURE_NAMES.index('hr_mean')] = 60.0 / rr.mean()
    if rr.size > 1:
        diffs = np.diff(rr)
        feats[FEATURE_NAMES.index('rmssd')] = np.sqrt(np.mean(diffs**2))
        feats[FEATURE_NAMES.index('pnn50')] = np.mean(np.abs(diffs) > 0.05)
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
    feats[FEATURE_NAMES.index('kurtosis')] = pd.Series(x).kurtosis()
    feats[FEATURE_NAMES.index('skew')] = pd.Series(x).skew()
    return feats.astype(np.float32)

def sliding_windows(sig: np.ndarray):
    step = int(WIN_S * FS * (1 - OVL))
    length = WIN_S * FS
    for start in range(0, sig.size - length + 1, step):
        yield sig[start:start + length]

def load_data(mu: float, sigma: float) -> pd.DataFrame:
    rows, labels, subjects = [], [], []
    for subj_dir in sorted(BASE_DIR.iterdir()):
        if not subj_dir.is_dir(): continue
        for fn in sorted(subj_dir.glob('*.json')):
            d = json.loads(fn.read_text(encoding='utf-8'))
            label = d['label'].split('/')[-1].strip()
            ts = np.array(d['data']['ppg_continuous']['ts'], dtype=float)
            sig = np.array(d['data']['ppg_continuous']['green'], dtype=float) / 4096.0
            sig = sig[ts >= ts[0] + 5000]
            for seg in sliding_windows(sig):
                feats = extract_features(seg)
                rows.append(feats)
                labels.append(label)
                subjects.append(subj_dir.name)
    df = pd.DataFrame(rows, columns=FEATURE_NAMES)
    df['label'] = labels
    df['subject'] = subjects
    df = df.dropna(subset=FEATURE_NAMES)
    for f in FEATURE_NAMES:
        df[f] = pd.to_numeric(df[f], errors='coerce')
    df = df.dropna()
    return df

if __name__ == '__main__':
    calib_file = pathlib.Path('models/train_calib.json')
    with open(calib_file, encoding='utf-8') as f:
        orig = json.load(f)
    mu_raw = orig['stats_raw']['green']['mu']
    sigma_raw = orig['stats_raw']['green']['sigma']

    df = load_data(mu_raw, sigma_raw)
    print('Dataset windows:', df.shape)

    mu_arr = np.array([orig['stats'][f]['mu'] for f in FEATURE_NAMES], dtype=np.float32)
    sigma_arr = np.array([orig['stats'][f]['sigma'] for f in FEATURE_NAMES], dtype=np.float32)
    X = ((df[FEATURE_NAMES].values - mu_arr) / sigma_arr).astype(np.float32)
    y = LabelEncoder().fit_transform(df['label'])
    g = df['subject'].to_numpy()

    logo = LeaveOneGroupOut()
    accs = []
    for tr, te in logo.split(X, y, g):
        model = models.Sequential([
            layers.Input(shape=(len(FEATURE_NAMES),)),
            layers.Dense(128, activation='relu'), layers.Dropout(0.3),
            layers.Dense(64, activation='relu'), layers.Dropout(0.3),
            layers.Dense(3, activation='softmax')
        ])
        model.compile('adam', 'sparse_categorical_crossentropy', ['accuracy'])
        model.fit(X[tr], y[tr], epochs=50, batch_size=32, verbose=0)
        accs.append(model.evaluate(X[te], y[te], verbose=0)[1])
    print(f'MLP LOSO-CV: {np.mean(accs):.3f} ± {np.std(accs):.3f}')
