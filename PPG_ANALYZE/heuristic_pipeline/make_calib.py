#!/usr/bin/env python
# make_calib.py  — subject01 recordings → calib.json
import json, re, unicodedata
from pathlib import Path
import numpy as np, pandas as pd
from scipy.signal import butter, filtfilt

BASE_DIR = Path(r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings")
OUT_JSON = BASE_DIR / "calib.json"
FS, WIN, STRIDE = 25, 10, 1       # Hz, seconds

LABEL_MAP = {
    "standing": "standing",
    "upright‑sitting": "upright-sitting",
    "upright-sitting": "upright-sitting",
    "supine‑lying": "supine-lying",
    "supine-lying": "supine-lying",
}

def norm_label(s: str) -> str:
    if "/" in s: s = s.split("/")[-1]
    s = unicodedata.normalize("NFKC", s.strip()).lower()
    s = s.replace(" ", "")
    return LABEL_MAP.get(s, s)

# ---------- signal helpers ----------
b, a = butter(3, [0.5/(0.5*FS), 5/(0.5*FS)], "band")

def detrend(x):
    idx = np.arange(x.size)
    slope, intercept = np.polyfit(idx, x, 1)
    return x - (slope*idx + intercept)

def extract_feats(x):
    out = {}
    pk = np.where((x[1:-1] > x[:-2]) & (x[1:-1] > x[2:]))[0] + 1
    rr = np.diff(pk) / FS
    out["n_peaks"] = pk.size
    if rr.size:
        out["rr_mean"] = rr.mean()
        out["hr_mean"] = 60/rr.mean()
    if rr.size > 1:
        diff = np.diff(rr)
        out["rmssd"]  = np.sqrt((diff**2).mean())
        out["pnn50"]  = (np.abs(diff) > .05).mean()
    tr = np.where((x[1:-1] < x[:-2]) & (x[1:-1] < x[2:]))[0] + 1
    cyc = []
    for p in pk:
        f = tr[tr < p]
        n = tr[tr > p]
        if f.size and n.size:
            cyc.append((f[-1], p, n[0]))
    if cyc:
        crest = np.mean([(p-f)/FS for f,p,_ in cyc])
        dwell = np.mean([(n-f)/FS for f,_,n in cyc])
        out.update({"crest_t": crest, "dwell_t": dwell,
                    "pwtf": crest/dwell if dwell else np.nan})
    s = pd.Series(x)
    out["skew"]     = s.skew()
    out["kurtosis"] = s.kurtosis()
    return out

# ---------- iterate recordings ----------
rows = []
for fp in BASE_DIR.rglob("*.json"):
    jd = json.loads(Path(fp).read_text(encoding="utf-8"))
    lab = norm_label(jd["label"])
    g   = np.asarray(jd["data"]["ppg_continuous"]["green"], np.float32)/4096
    step, win = STRIDE*FS, WIN*FS
    for st in range(0, g.size - win + 1, step):
        seg = filtfilt(b, a, detrend(g[st:st+win]))
        rows.append({**extract_feats(seg), "label": lab})

df = pd.DataFrame(rows)
print("windows:", df.shape)

def stat(f):
    return {"mu": float(df[f].mean()), "sigma": float(df[f].std(ddof=0))}

calib = {
    "hr_mean":  stat("hr_mean"),
    "pnn50":    stat("pnn50"),
    "pwtf":     stat("pwtf"),
    "kurtosis": stat("kurtosis"),
    "thresholds": {
        "supine":   {"hr_z_max": -0.8, "pnn50_z_min": 0.8},
        "standing": {"hr_z_min": 0.8,  "kurtosis_z_min": 0.5}
    }
}
OUT_JSON.write_text(json.dumps(calib, indent=2), encoding="utf-8")
print("✅ calib.json saved →", OUT_JSON)
