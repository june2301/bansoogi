#!/usr/bin/env python
# make_calib_by_label.py — recordings 폴더의 JSON → calib.json 생성
import json, unicodedata
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks

# ─────────────── 설정 ───────────────
BASE_DIR   = Path(r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings")
OUT_JSON   = BASE_DIR / "calib.json"
FS         = 25      # Hz
WARMUP_SEC = 5       # sec
WIN_SEC    = 10      # sec
STRIDE_SEC = 1       # sec

LABEL_MAP = {
    "standing":         "standing",
    "upright‑sitting":  "upright-sitting",
    "upright-sitting":  "upright-sitting",
    "supine‑lying":     "supine-lying",
    "supine-lying":     "supine-lying",
}

def norm_label(s: str) -> str:
    s = unicodedata.normalize("NFKC", s)
    s = s.replace("\u2011", "-").lower().split("/")[-1].strip()
    return LABEL_MAP.get(s.replace(" ", ""), s)

def butter_bandpass_filter(sig, fs, lowcut=0.5, highcut=5.0, order=3):
    nyq = 0.5*fs
    b, a = butter(order, [lowcut/nyq, highcut/nyq], btype='band')
    return filtfilt(b, a, sig)

def preprocess_signal(ts, sig, warmup_sec=WARMUP_SEC, fs=FS, apply_filter=True):
    start_time = ts[0] + warmup_sec*1000
    mask = ts >= start_time
    ts_c, sig_c = ts[mask], sig[mask]
    idx = np.arange(len(sig_c))
    trend = np.polyval(np.polyfit(idx, sig_c, 1), idx)
    sig_c = sig_c - trend
    if apply_filter:
        sig_c = butter_bandpass_filter(sig_c, fs)
    return ts_c, sig_c

def extract_features(ts, sig, fs=FS):
    feats = {
        'n_samples': len(sig),
        'mean':      np.mean(sig),
        'std':       np.std(sig),
        'min':       np.min(sig),
        'max':       np.max(sig),
        'diff_std':  np.std(np.diff(sig)),
        'skew':      pd.Series(sig).skew(),
        'kurtosis':  pd.Series(sig).kurtosis(),
    }
    peaks, _ = find_peaks(sig, distance=fs*0.4)
    rr = np.diff(ts[peaks]) / 1000.0 if len(peaks)>1 else np.array([])
    feats.update({
        'n_peaks': len(peaks),
        'hr_mean': 60.0/rr.mean() if rr.size>0 else np.nan,
        'rr_mean': rr.mean()     if rr.size>0 else np.nan,
        'rr_std':  rr.std()      if rr.size>0 else np.nan,
        'rmssd':   np.sqrt(np.mean(np.diff(rr)**2)) if rr.size>1 else np.nan,
        'pnn50':   np.sum(np.abs(np.diff(rr))>0.05)/len(rr) if rr.size>1 else np.nan
    })
    return feats

# ─── 전처리 후 윈도우별 피처 추출 ───
rows = []
for fp in BASE_DIR.rglob("*.json"):
    jd = json.loads(fp.read_text(encoding='utf-8'))
    if 'label' not in jd or 'data' not in jd:
        continue
    label = norm_label(jd['label'])
    ts_full = np.array(jd['data']['ppg_continuous']['ts'], dtype=np.int64)
    g_full  = np.array(jd['data']['ppg_continuous']['green'], dtype=float) / 4096.0

    ts_clean, sig_clean = preprocess_signal(ts_full, g_full)
    step = STRIDE_SEC * FS
    win  = WIN_SEC    * FS
    for start in range(0, len(sig_clean) - win + 1, step):
        seg_ts = ts_clean[start:start+win]
        seg_sg = sig_clean[start:start+win]
        feats = extract_features(seg_ts, seg_sg)
        feats['label'] = label
        rows.append(feats)

df = pd.DataFrame(rows)
print(f"총 윈도우 수: {len(df)}")

# ─── 라벨별 통계(stats)───
CANDIDATE_LABEL_FEATURES = ['hr_mean','pnn50','pwtf','kurtosis']
LABEL_FEATURES = [f for f in CANDIDATE_LABEL_FEATURES if f in df.columns]
stats = {}
for lab, sub in df.groupby('label'):
    stats[lab] = {
        feat: {
            'mu':    float(sub[feat].mean(skipna=True)),
            'sigma': float(sub[feat].std(ddof=0, skipna=True))
        }
        for feat in LABEL_FEATURES
    }

# ─── 전체 윈도우 통합 평균(calib_means)───
ALL_FEATURES = ['pnn50','rr_mean','hr_mean','rmssd','n_peaks',
                'crest_t','dwell_t','pwtf','kurtosis','skew']
available = [f for f in ALL_FEATURES if f in df.columns]
calib_means = df[available].mean(skipna=True).to_dict()

# ─── JSON 작성 & 저장 ───
out = {
    "stats": stats,
    "thresholds": {
        "supine":   {"hr_z_max": -0.8, "pnn50_z_min": 0.8},
        "standing": {"hr_z_min":  0.8, "kurtosis_z_min": 0.5}
    },
    "calib_means": {k: float(v) for k,v in calib_means.items()}
}

OUT_JSON.write_text(json.dumps(out, indent=2), encoding='utf-8')
print(f"✅ calib.json 생성 완료 → {OUT_JSON}")
