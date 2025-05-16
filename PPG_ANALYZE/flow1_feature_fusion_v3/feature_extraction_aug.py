# -*- coding: utf-8 -*-
"""flow1_feature_fusion_v3 · Fix: Augment raw & features together (2025‑05‑16)"""
import os, json, math, random, numpy as np, pandas as pd
from scipy.signal import butter, filtfilt, welch, find_peaks
from scipy.stats import skew, kurtosis
from configs import *

# -------------------- DSP helpers --------------------

def bp(sig, band, order=4):
    nyq = 0.5 * FS
    b, a = butter(order, [band[0] / nyq, band[1] / nyq], btype="band")
    return filtfilt(b, a, sig)

def sqi(sig):
    ac = sig - sig.mean(); rms = math.sqrt(np.mean(ac**2))
    b, a = butter(2, BAND_RAW[1] / (0.5 * FS), btype="high")
    noise = filtfilt(b, a, sig)
    return 20 * math.log10(rms / (math.sqrt(np.mean(noise**2)) + 1e-8))

# -------------------- window + jitter ----------------
STEP = int(WIN_FEAT_SEC * FS * (1 - OVERLAP_RATE))
JITTER_MAX = STEP // 4  # 25 % jitter (더 안정적)

def win_jitter(sig):
    n = int(WIN_FEAT_SEC * FS)
    for start in range(0, len(sig) - n + 1, STEP):
        j = random.randint(-JITTER_MAX, JITTER_MAX)
        s = max(0, min(len(sig) - n, start + j))
        yield sig[s : s + n]

# -------------------- feature extraction -------------

def morph(w):
    d1 = np.gradient(w); d2 = np.gradient(d1)
    peaks,_ = find_peaks(w, distance=int(0.2 * FS))
    rr = np.diff(peaks) * (1000 / FS) if len(peaks) > 1 else [0]
    return {
        "dc": w.mean(),
        "amp": w.ptp(),
        "rms": math.sqrt(np.mean(w**2)),
        "skew": skew(w),
        "kurt": kurtosis(w),
        "zcr": np.sum(w[:-1] * w[1:] < 0) / len(w),
        "d1": d1.mean(),
        "d2": d2.mean(),
        "hr": 60000 / np.mean(rr) if rr[0] else 0,
        "rmssd": math.sqrt(np.mean(np.diff(rr)**2)) if len(rr) > 2 else 0,
    }

def spec(w):
    f, p = welch(w, fs=FS, nperseg=len(w))
    tot = np.trapz(p, f) + 1e-9
    band_r = (f >= 0.1) & (f <= 0.4); band_m = (f >= 0.4) & (f <= 2.0)
    return {
        "psd_r": np.trapz(p[band_r], f[band_r]) / tot,
        "psd_m": np.trapz(p[band_m], f[band_m]) / tot,
        "ratio": (np.trapz(p[band_r], f[band_r]) + 1e-9) / (np.trapz(p[band_m], f[band_m]) + 1e-9),
        "ent": -np.sum((p / tot) * np.log(p / tot + 1e-12)),
    }

# -------------------- augmentation -------------------

def aug_noise(x):
    return x + np.random.normal(0, AUG_GAUSS_STD, x.shape)

def aug_scale(x):
    return x * np.random.uniform(1 - AUG_MAG_SCALE, 1 + AUG_MAG_SCALE)

# -------------------- build dataset ------------------

def build(root="recordings", out="flow1_v3_data.npz"):
    raws, feats, labels, subs = [], [], [], []
    n_raw = int(WIN_RAW_SEC * FS)
    for s in sorted(os.listdir(root)):
        d = os.path.join(root, s)
        if not os.path.isdir(d):
            continue
        for fn in sorted(os.listdir(d)):
            if not fn.endswith(".json"):
                continue
            data = json.load(open(os.path.join(d, fn), "r", encoding="utf-8"))
            sig = np.asarray(data["data"]["ppg_continuous"]["green"], dtype=float)
            label = fn.split("---")[-1].replace(".json", "")
            sig_raw = bp(sig, BAND_RAW); sig_feat = bp(sig, BAND_FEAT)

            for w_feat in win_jitter(sig_feat):
                if sqi(w_feat) < SQI_THRESH:
                    continue
                # base raw window crop
                base_raw = sig_raw[:n_raw]
                if len(base_raw) < n_raw:
                    base_raw = np.pad(base_raw, (0, n_raw - len(base_raw)), mode="edge")

                # list of tuples (raw_segment, feature_window)
                pairs = [(base_raw, w_feat)]
                for _ in range(AUG_REPEAT):
                    w_aug_feat = aug_scale(aug_noise(w_feat))
                    raw_aug = aug_scale(aug_noise(base_raw))
                    pairs.append((raw_aug, w_aug_feat))

                for r, fwin in pairs:
                    raws.append(r)
                    feats.append({**morph(fwin), **spec(fwin)})
                    labels.append(label); subs.append(s)

    Xr = np.stack(raws).astype("float32").reshape(-1, n_raw, 1)
    Xf = pd.DataFrame(feats).fillna(0).astype("float32").values
    np.savez_compressed(out, raw=Xr, feat=Xf,
                        labels=np.array(labels), subjects=np.array(subs))
    print(f"Saved {out} | samples={len(labels)} | raw={Xr.shape} | feat={Xf.shape}")

if __name__ == "__main__":
    build()
