import os, json, math, sys
import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, welch, find_peaks
from scipy.stats import skew, kurtosis
from typing import List, Tuple

from configs import (
    FS, WIN_RAW_SEC, WIN_FEAT_SEC, OVERLAP_RATE,
    BANDPASS_RAW, BANDPASS_FEAT, SQI_THRESH, DEBUG
)

# ---------------------------------------------------------------
# 1. DSP 유틸
# ---------------------------------------------------------------

_DEF_ORDER = 4


def _bp(sig: np.ndarray, band: Tuple[float, float]) -> np.ndarray:
    nyq = FS * 0.5
    b, a = butter(_DEF_ORDER, [band[0] / nyq, band[1] / nyq], btype="band")
    return filtfilt(b, a, sig)


def _sqi(sig: np.ndarray) -> float:
    ac = sig - sig.mean()
    rms_sig = math.sqrt(np.mean(ac ** 2))
    b, a = butter(2, BANDPASS_RAW[1] / (0.5 * FS), btype="high")
    noise = filtfilt(b, a, sig)
    rms_noise = math.sqrt(np.mean(noise ** 2))
    if rms_noise == 0:
        return np.inf
    return 20 * math.log10(rms_sig / rms_noise)


def _windows(sig: np.ndarray, win_sec: int) -> List[np.ndarray]:
    n = int(win_sec * FS)
    step = int(n * (1 - OVERLAP_RATE))
    return [sig[i : i + n] for i in range(0, len(sig) - n + 1, step)]

# ---------------------------------------------------------------
# 2. 피처 추출
# ---------------------------------------------------------------

def _feat_morph(w: np.ndarray) -> dict:
    d = {
        "dc": w.mean(),
        "ac_amp": w.max() - w.min(),
        "rms": math.sqrt(np.mean(w ** 2)),
        "skew": skew(w),
        "kurtosis": kurtosis(w),
        "zcr": np.sum(w[:-1] * w[1:] < 0) / len(w),
    }
    peaks, _ = find_peaks(w, distance=int(0.2 * FS))
    if len(peaks) >= 2:
        rr = np.diff(peaks) * (1000 / FS)
        d["hr"] = 60_000 / rr.mean()
        d["rmssd"] = math.sqrt(np.mean(np.diff(rr) ** 2))
    else:
        d["hr"], d["rmssd"] = 0.0, 0.0
    return d


def _feat_spectral(w: np.ndarray) -> dict:
    freqs, psd = welch(w, fs=FS, nperseg=len(w))
    band_resp = (freqs >= 0.1) & (freqs <= 0.4)
    band_motion = (freqs >= 0.4) & (freqs <= 2.0)
    total = np.trapz(psd, freqs) + 1e-9
    return {
        "psd_resp": np.trapz(psd[band_resp], freqs[band_resp]) / total,
        "psd_motion": np.trapz(psd[band_motion], freqs[band_motion]) / total,
        "spec_entropy": -np.sum((psd / total) * np.log(psd / total + 1e-12)),
    }

# ---------------------------------------------------------------
# 3. 메인
# ---------------------------------------------------------------

def build_npz(root="recordings", out_file="flow1_data.npz") -> None:
    raws, feats, labels, subjects = [], [], [], []

    for subj in sorted(os.listdir(root)):
        subj_dir = os.path.join(root, subj)
        if not os.path.isdir(subj_dir):
            continue
        for fn in sorted(os.listdir(subj_dir)):
            if not fn.endswith(".json"):
                continue
            path = os.path.join(subj_dir, fn)
            with open(path, "r", encoding="utf-8") as f:
                payload = json.load(f)
            sig = np.array(payload["data"]["ppg_continuous"]["green"], dtype=float)
            label = fn.split("---")[-1].replace(".json", "")

                        # Unified windows: use feature-based windows for consistency
            sig_feat = _bp(sig, BANDPASS_FEAT)
            feat_windows = _windows(sig_feat, WIN_FEAT_SEC)
            raw_bp = _bp(sig, BANDPASS_RAW)
            raw_win_len = int(WIN_RAW_SEC * FS)
            for w_feat in feat_windows:
                if _sqi(w_feat) < SQI_THRESH:
                    continue
                # Extract features from the full feature window
                morph = _feat_morph(w_feat)
                spec = _feat_spectral(w_feat)
                feats.append({**morph, **spec})
                # Crop the raw bandpassed signal to the first WIN_RAW_SEC seconds
                raw_crop = raw_bp[:raw_win_len] if len(raw_bp) >= raw_win_len else np.pad(raw_bp, (0, raw_win_len - len(raw_bp)))
                raws.append(raw_crop)
                labels.append(label)
                subjects.append(subj)

            

    X_raw = np.stack(raws).astype("float32")  # (N, 250)
    df_feat = pd.DataFrame(feats).fillna(0).astype("float32")
    np.savez_compressed(
        out_file,
        raw=X_raw,
        features=df_feat.values,
        feat_names=df_feat.columns.values,
        labels=np.array(labels),
        subjects=np.array(subjects),
    )
    print(f"★ Saved {out_file}  |  windows={X_raw.shape[0]}")

if __name__ == "__main__":
    build_npz()