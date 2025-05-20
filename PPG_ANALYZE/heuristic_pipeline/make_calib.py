#!/usr/bin/env python
# make_calib_by_label.py — recordings 폴더의 JSON → calib.json 생성 (라벨별 stats + 전체 평균 calib_means)

import json
import unicodedata
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt

# ─────────────── 설정 ───────────────
BASE_DIR = Path(r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings")
OUT_JSON = BASE_DIR / "calib.json"
FS, WIN_SEC, STRIDE_SEC = 25, 10, 1  # Hz, seconds

LABEL_MAP = {
    "standing":         "standing",
    "upright‑sitting":  "upright-sitting",
    "upright-sitting":  "upright-sitting",
    "supine‑lying":     "supine-lying",
    "supine-lying":     "supine-lying",
}

def norm_label(s: str) -> str:
    """non‑breaking hyphen 등을 ASCII '-'로 바꿔 일관된 라벨 생성"""
    s = unicodedata.normalize("NFKC", s)
    s = s.replace("\u2011", "-").lower().split("/")[-1].strip()
    return LABEL_MAP.get(s.replace(" ", ""), s)

# ─── 필터 계수 ───
b, a = butter(3, [0.5/(0.5*FS), 5/(0.5*FS)], btype="band")

def detrend(x: np.ndarray) -> np.ndarray:
    idx = np.arange(x.size)
    slope, intercept = np.polyfit(idx, x, 1)
    return x - (slope*idx + intercept)

def extract_feats(x: np.ndarray, ts: np.ndarray) -> dict:
    """윈도우 신호 x, 타임스탬프 ts 로부터 10개 피처 계산"""
    out = {
        "pnn50": np.nan, "rr_mean": np.nan, "hr_mean": np.nan,
        "rmssd": np.nan, "n_peaks": 0,
        "crest_t": np.nan, "dwell_t": np.nan, "pwtf": np.nan,
        "kurtosis": np.nan, "skew": np.nan
    }
    # 피크/트로프 인덱스
    pk = np.where((x[1:-1] > x[:-2]) & (x[1:-1] > x[2:]))[0] + 1
    tr = np.where((x[1:-1] < x[:-2]) & (x[1:-1] < x[2:]))[0] + 1
    out["n_peaks"] = int(pk.size)

    # HRV 계산
    if pk.size >= 2:
        times = ts[pk]
        rr = np.diff(times) / 1000.0
        out["rr_mean"] = float(rr.mean())
        out["hr_mean"] = float(60.0 / rr.mean()) if rr.mean() > 0 else np.nan
        diffs = np.diff(rr)
        out["rmssd"] = float(np.sqrt((diffs**2).mean()))
        out["pnn50"] = float((np.abs(diffs) > 0.05).mean())

    # Crest/Dwell/PWTF
    cycles = []
    for p in pk:
        f = tr[tr < p]
        n = tr[tr > p]
        if f.size and n.size:
            cycles.append((f[-1], p, n[0]))
    if cycles:
        crest = np.mean([(p - f)/FS for f, p, _ in cycles])
        dwell = np.mean([(n - f)/FS for f, _, n in cycles])
        out.update({
            "crest_t": float(crest),
            "dwell_t": float(dwell),
            "pwtf": float(crest/dwell) if dwell else np.nan
        })

    # 왜도·첨도
    series = pd.Series(x)
    out["skew"] = float(series.skew())
    out["kurtosis"] = float(series.kurtosis())

    return out

# ─── 윈도우 단위 피처 계산 ───
rows = []
for fp in BASE_DIR.rglob("*.json"):
    jd = json.loads(fp.read_text(encoding="utf-8"))
    if "label" not in jd or "data" not in jd:
        continue

    lab = norm_label(jd["label"])
    ts_full = np.array(jd["data"]["ppg_continuous"]["ts"], dtype=np.int64)
    g_full  = np.array(jd["data"]["ppg_continuous"]["green"], dtype=float) / 4096.0

    # 처음 5초 워밍업 제거
    mask = ts_full >= (ts_full[0] + 5_000)
    ts = ts_full[mask]
    g  = g_full[mask]

    step = STRIDE_SEC * FS
    win  = WIN_SEC * FS
    for st in range(0, len(g) - win + 1, step):
        seg_ts = ts[st:st+win]
        seg_g  = g[st:st+win]
        filt = filtfilt(b, a, detrend(seg_g))
        feats = extract_feats(filt, seg_ts)
        feats["label"] = lab
        rows.append(feats)

# DataFrame 생성
df = pd.DataFrame(rows)
print(f"총 윈도우 수: {df.shape[0]}")

# ─── 라벨별 μ/σ 계산 ───
LABEL_FEATURES = ["hr_mean", "pnn50", "pwtf", "kurtosis"]
stats = {}
for label, sub in df.groupby("label"):
    stats[label] = {
        feat: {
            "mu":    float(sub[feat].mean(skipna=True)),
            "sigma": float(sub[feat].std(ddof=0, skipna=True))
        }
        for feat in LABEL_FEATURES
    }

# ─── 전체 윈도우 통합 평균 (calib_means) 계산 ───
ALL_FEATURES = ["pnn50","rr_mean","hr_mean","rmssd","n_peaks",
                "crest_t","dwell_t","pwtf","kurtosis","skew"]
calib_means = df[ALL_FEATURES].mean(skipna=True).to_dict()

# ─── JSON 구조 작성 & 저장 ───
calib = {
    "stats": stats,
    "thresholds": {
        "supine":   {"hr_z_max": -0.8, "pnn50_z_min": 0.8},
        "standing": {"hr_z_min":  0.8, "kurtosis_z_min": 0.5}
    },
    "calib_means": { k: float(v) for k, v in calib_means.items() }
}

OUT_JSON.write_text(json.dumps(calib, indent=2), encoding="utf-8")
print(f"✅ calib.json 저장 → {OUT_JSON}")
