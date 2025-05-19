#!/usr/bin/env python
# train_ppg_model_with_calib.py
# pip install numpy pandas scipy scikit-learn tensorflow==2.15.0

import json
import os
import pathlib
import warnings

import numpy as np
import pandas as pd
import scipy
import tensorflow as tf
from scipy.signal import butter, filtfilt, find_peaks, welch
from scipy.stats import kurtosis, skew
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import LeaveOneGroupOut, RandomizedSearchCV, cross_val_score
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.pipeline import Pipeline
from tensorflow.keras import layers, models, callbacks

# ─────────────────────────────── 설정 ───────────────────────────────
BASE_DIR = pathlib.Path("recordings")        # JSON recordings 폴더
WIN_S = 15                                   # 윈도우 길이(초)
OVL = 0.3                                    # 오버랩 비율
FS = 25                                      # 샘플링 주파수
LABELS = ['upright-sitting', 'supine-lying', 'standing']
FEATS_BASIC = [
    'pnn50','rr_mean','hr_mean','rmssd','n_peaks',
    'crest_t','dwell_t','pwtf','kurtosis','skew'
]
# 추가 스펙트럼 파워 피처: 저·중·고 밴드
FEATS_SPECTRAL = ['power_0.5_1.5','power_1.5_3','power_3_5']
FEATURES = FEATS_BASIC + FEATS_SPECTRAL
BINS = 10                                   # 역매핑 빈 수

warnings.filterwarnings("ignore")

# ────────────────── 전처리 & 피처 추출 ───────────────────
def _band(x, fs=FS, lo=0.5, hi=5.0, order=3):
    nyq = 0.5 * fs
    b, a = butter(order, [lo/nyq, hi/nyq], btype='band')
    return filtfilt(b, a, x)


def preprocess_signal(ts, sig, mu, sigma, fs=FS):
    # 5초 워밍업 제거
    sig = sig[ts >= ts[0] + 5000] / 4096.0
    # detrend
    idx = np.arange(len(sig))
    trend = np.polyval(np.polyfit(idx, sig, 1), idx)
    detrended = sig - trend
    # z-score
    normed = (detrended - mu) / sigma if sigma > 0 else detrended - mu
    # 밴드패스
    return _band(normed, fs)


def extract_features(x, fs=FS):
    feats = {}
    # HRV-10 기본 피처
    pk, _ = find_peaks(x, distance=int(fs*0.4))
    tr, _ = find_peaks(-x, distance=int(fs*0.4))
    rr = np.diff(pk)/fs if len(pk)>1 else np.array([])
    diffs = np.diff(rr) if len(rr)>1 else np.array([])
    feats['n_peaks'] = len(pk)
    feats['rr_mean'] = rr.mean() if rr.size else np.nan
    feats['hr_mean'] = 60./feats['rr_mean'] if feats['rr_mean']>0 else np.nan
    feats['rmssd'] = np.sqrt(np.mean(diffs**2)) if diffs.size else np.nan
    feats['pnn50'] = np.mean(np.abs(diffs)>0.05) if diffs.size else np.nan
    # crest/dwell/pwtf
    cyc = [(pre.max(), p, nex.min()) for p in pk for pre in [tr[tr<p]] for nex in [tr[tr>p]] if pre.size and nex.size]
    if cyc:
        ct = np.mean([(p-f)/fs for f,p,_ in cyc])
        dw = np.mean([(n-f)/fs for f,_,n in cyc])
        feats['crest_t']=ct; feats['dwell_t']=dw; feats['pwtf']=ct/dw if dw else np.nan
    else:
        feats.update(dict.fromkeys(['crest_t','dwell_t','pwtf'], np.nan))
    feats['kurtosis'] = kurtosis(x) if x.size else np.nan
    feats['skew']     = skew(x) if x.size else np.nan
    # 스펙트럼 파워
    f, Pxx = welch(x, fs=fs, nperseg=len(x))
    for (lo,hi),name in zip([(0.5,1.5),(1.5,3.0),(3.0,5.0)], FEATS_SPECTRAL):
        mask = (f>=lo)&(f<hi)
        feats[name] = np.trapz(Pxx[mask], f[mask]) if mask.any() else np.nan
    return feats


def sliding_windows(sig, win_s, ovl, fs=FS):
    step = int(win_s*(1-ovl)*fs)
    n = int(win_s*fs)
    for start in range(0, len(sig)-n+1, step): yield sig[start:start+n]

# ─────────────────── 데이터 로딩 ───────────────────
def load_dataset(base_dir, mu, sigma, win_s, ovl, fs):
    rows=[]
    for subj in sorted(pathlib.Path(base_dir).iterdir()):
        if not subj.is_dir(): continue
        for fn in sorted(subj.glob('*.json')):
            d = json.loads(fn.read_text(encoding='utf-8'))
            label = fn.stem.split('---')[-1]
            ts = np.array(d['data']['ppg_continuous']['ts'], float)
            raw = np.array(d['data']['ppg_continuous']['green'], float)
            sig = preprocess_signal(ts, raw, mu, sigma, fs)
            for seg in sliding_windows(sig, win_s, ovl, fs):
                feats = extract_features(seg, fs)
                feats.update({'label':label,'subject':subj.name})
                rows.append(feats)
    df = pd.DataFrame(rows).dropna(subset=['label'])
    return df

# ───────────────── 부스트된 Random Forest ─────────────────
def tune_and_evaluate_rf(X, y, g):
    param_dist = {
        'n_estimators': [50,100,200],
        'max_depth': [None,5,10,20],
        'min_samples_split': [2,5,10]
    }
    base = RandomForestClassifier(random_state=0)
    logo = LeaveOneGroupOut()
    search = RandomizedSearchCV(
        base, param_dist, n_iter=10, cv=logo.split(X,y,g), scoring='accuracy', n_jobs=-1, random_state=0
    )
    search.fit(X,y)
    best = search.best_estimator_
    scores = cross_val_score(best, X, y, cv=logo.split(X,y,g), scoring='accuracy')
    print(f"RF tuned LOSO-CV: {scores.mean():.3f} ± {scores.std():.3f}")
    print("Best params:", search.best_params_)

# ─────────────────────────── main ───────────────────────────
if __name__=='__main__':
    cf = pathlib.Path('models/train_calib.json')
    if not cf.exists() or cf.stat().st_size==0: raise FileNotFoundError(cf)
    orig = json.loads(cf.read_text())
    mu = orig['stats_raw']['green']['mu']; sigma = orig['stats_raw']['green']['sigma']

    df = load_dataset(BASE_DIR, mu, sigma, WIN_S, OVL, FS)
    print("Dataset windows:", df.shape)

    # 표준 스케일링
    scaler = StandardScaler()
    X = scaler.fit_transform(df[FEATURES].fillna(0))
    y = LabelEncoder().fit_transform(df['label'])
    g = df['subject'].to_numpy()

    # 튜닝된 RF 평가
    tune_and_evaluate_rf(X, y, g)
