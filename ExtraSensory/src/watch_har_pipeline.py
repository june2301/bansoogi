# -*- coding: utf-8 -*-
"""watch_har_pipeline.py · 2025‑05‑07
───────────────────────────────────────────────────────────────────────────────
End‑to‑end pipeline
  1) ExtraSensory  .dat  → absolute ms timestamp 보정(+mg→m/s²)
  2) CAPTURE‑24    .csv → g→m/s²  + ‘sleep’ 키워드 라벨 파싱
  3) 25 Hz resample · 5 s / 50 % OL · sitting=0 / lying=1
  4) data/npz/watch_har_windows.npz  저장
  5) 1‑D CNN 학습 → model/sitlie_fp32.tflite
"""
from __future__ import annotations
import argparse, math
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, decimate
import tensorflow as tf
from tqdm import tqdm

# ───────────────────────────────────────── PATHS
EXS_ROOT       = Path("data/exs_raw")                     # <uuid>/*.dat
EXS_LABEL_ROOT = Path("data/features_labels/users")       # <uuid>.features_labels.csv(.gz)
CAP_ROOT       = Path("data/cap_raw")                     # P00X.csv
NPZ_PATH       = Path("data/npz/watch_har_windows.npz")
MODEL_DIR      = Path("model")

# ───────────────────────────────────────── WINDOW / RESAMPLE
SAMPLE_OUT = 25  # Hz
WINDOW_SEC = 5
OVERLAP    = 0.5
WIN_SIZE   = int(WINDOW_SEC * SAMPLE_OUT)          # 125
STRIDE     = int(WIN_SIZE * (1 - OVERLAP))         # 62
SMA_TH     = 0.30                                   # g – dynamic‑skip

# ───────────────────────────────────────── DSP UTIL
def butter_lowpass(arr: np.ndarray, fs_in: int,
                   cutoff: float = 12, order: int = 4):
    b, a = butter(order, cutoff / (0.5 * fs_in), btype="low")
    return filtfilt(b, a, arr, axis=0)

def resample_acc(arr: np.ndarray, fs_in: int) -> np.ndarray:
    if fs_in == SAMPLE_OUT:
        return arr
    arr_lp = butter_lowpass(arr, fs_in)
    q      = int(fs_in / SAMPLE_OUT)      # fs_in 은 25·50·100 등 → 정수배
    return decimate(arr_lp, q, axis=0)

# ───────────────────────────────────────── ExtraSensory 라벨 dict
def _exs_label_dict(uuid: str) -> dict[int, int | None]:
    path = next(EXS_LABEL_ROOT.glob(f"{uuid}.features_labels.csv*"), None)
    if path is None:
        raise FileNotFoundError(f"{uuid}.features_labels.csv(.gz) not found")
    df = pd.read_csv(path, compression="gzip" if path.suffix == ".gz" else None)
    keep = df[["timestamp", "label:LYING_DOWN", "label:SITTING"]]
    return {int(r.timestamp):
                1 if r["label:LYING_DOWN"]==1 else
                0 if r["label:SITTING"]   ==1 else None
            for _, r in keep.iterrows()}

# ───────────────────────────────────────── LOADERS
def iter_exs_dat(root: Path):
    """
    yield ts_ms[N], acc[N,3], uuid, file_path
    - ts 열은 *상대(ms)* → 파일명에 있는 epoch 초로 보정
    - acc 열은 mg → m/s²
    """
    for dat in root.rglob("*.dat"):
        uuid = dat.parent.name
        df   = pd.read_csv(dat, sep=r"\s+", header=None, engine="python")
        if df.shape[1] == 3:                       # ts 없는 파일 → skip
            continue

        base_sec = int(dat.stem.split('.')[0])     # 1449601717
        abs_ts   = df.iloc[:,0].astype(np.float64).values + base_sec*1000
        acc_ms2  = df.iloc[:,1:4].astype(np.float32).values * (9.81/1000)
        yield abs_ts, acc_ms2, uuid, dat

# ────────────────────────── CAPTURE‑24 loader (fixed)
def iter_cap_csv(root: Path):
    """
    yield ts_ms[N], acc[N,3], annos[N], file
    - 폴더명 ‘P001.csv/’ 같이 *.csv 디렉터리 스킵
    - 헤더 유무 자동 판별
    - g → m/s² 변환
    """
    for p in root.rglob("*.csv"):
        if not p.is_file():          # ★ ① 폴더 무시
            continue

        df = pd.read_csv(p, dtype={'annotation': str},
                         low_memory=False)

        # ★ ② 헤더 없으면 다시 읽기
        if not {'time','x','y','z','annotation'} <= set(df.columns):
            df = pd.read_csv(
                    p, header=None,
                    names=['time','x','y','z','annotation'],
                    dtype={'annotation': str},
                    low_memory=False)

        ts_ms  = pd.to_datetime(df['time']).astype('int64') // 1_000_000
        acc_ms = df[['x','y','z']].astype(np.float32).values * 9.81
        annos  = df['annotation'].astype(str).values
        yield ts_ms, acc_ms, annos, p


# ───────────────────────────────────────── HELPERS
def sliding_window(arr: np.ndarray, win: int, stride: int):
    for i in range(0, len(arr) - win + 1, stride):
        yield i, arr[i:i+win]

# after: gravity removal → dynamic acceleration
def sma_g(acc_win: np.ndarray) -> float:
    """
    dynamic Signal Magnitude Area (g 단위).
    각 축별 평균을 빼서 중력 성분을 제거한 뒤,
    |Δax| + |Δay| + |Δaz| 의 평균을 구함.
    """
    # 1) 윈도우별 축 평균 제거 (중력 성분 제거)
    acc_demean = acc_win - np.mean(acc_win, axis=0, keepdims=True)
    # 2) 각 샘플별 |dx|+|dy|+|dz|
    mag = np.sum(np.abs(acc_demean), axis=1)
    # 3) 윈도우 전체 평균 후 g 단위로 변환
    return float(np.mean(mag) / 9.81)

def _posture_from_text(t: str) -> int|None:
    t = t.lower()
    if 'lying' in t or 'sleep' in t: return 1
    if 'sit'   in t               : return 0
    return None

# ───────────────────────────────────────── PREPARE NPZ
def prepare_npz():
    X_buf, y_buf = [], []
    exs_cnt = cap_cnt = 0
    lbl_cache: dict[str, dict[int,int|None]] = {}

    # ---------- ExtraSensory
    for ts_ms, acc, uuid, _ in tqdm(iter_exs_dat(EXS_ROOT),
                                    desc="ExtraSensory", unit="file"):
        fs_in = int(round(1000 / np.median(np.diff(ts_ms))))
        acc25 = resample_acc(acc, fs_in)

        # ↓───────────────────────────────────────────
        # 1) 다운샘플 비율 계산
        step = max(1, fs_in // SAMPLE_OUT)
        # 2) 원본 ts_ms → 리샘플(ts_ms_rs), 길이 맞추기
        ts_ms_rs = ts_ms[::step][:len(acc25)]
        # ← 이 시점부터 acc25[i] 에 대응하는 ts_ms_rs[i] 가 보장됨
        # ↑───────────────────────────────────────────

        if uuid not in lbl_cache:
            lbl_cache[uuid] = _exs_label_dict(uuid)
        lmap = lbl_cache[uuid]

        for idx, win in sliding_window(acc25, WIN_SIZE, STRIDE):
            if sma_g(win) >= SMA_TH:        # dynamic skip
                continue

            # ↓ use resampled timestamp
            epoch_sec = int(ts_ms_rs[idx] // 1000)
            lab = lmap.get(epoch_sec)
            # ↑─────────────────────────────────────────

            if lab is None:                 # unlabeled
                continue

            X_buf.append(win)
            y_buf.append(lab)
            exs_cnt += 1

    # ---------- CAPTURE-24
    for ts_ms, acc, ann, _ in tqdm(iter_cap_csv(CAP_ROOT),
                                desc="CAPTURE-24", unit="file"):
        fs_in = int(round(1000 / np.median(np.diff(ts_ms))))
        acc25 = resample_acc(acc, fs_in)

        # ↓───────────────────────────────────────────
        step     = max(1, fs_in // SAMPLE_OUT)
        ts_ms_rs = ts_ms[::step][:len(acc25)]
        ann_rs   = ann[::step][:len(acc25)]
        # ↑───────────────────────────────────────────

        for idx, win in sliding_window(acc25, WIN_SIZE, STRIDE):
            if sma_g(win) >= SMA_TH:
                continue

            # ↓ use resampled annotation
            lab = _posture_from_text(ann_rs[idx])
            # ↑─────────────────────────────────────────

            if lab is None:
                continue

            X_buf.append(win)
            y_buf.append(lab)
            cap_cnt += 1


    print(f"[Debug] ExS windows={exs_cnt}, CAP windows={cap_cnt}")
    if not X_buf:
        raise RuntimeError("[!] no labeled windows found!")

    X = np.stack(X_buf).astype(np.float32)
    y = np.asarray(y_buf, np.int64)
    NPZ_PATH.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(NPZ_PATH, X=X, y=y)
    print(f"[✓] saved {X.shape} → {NPZ_PATH}")

# ───────────────────────────────────────── MODEL
def build_model(input_shape=(WIN_SIZE,3)):
    m = tf.keras.Sequential([
        tf.keras.layers.Input(shape=input_shape),
        tf.keras.layers.Conv1D(32,5,activation='relu'),
        tf.keras.layers.Conv1D(64,5,activation='relu'),
        tf.keras.layers.GlobalAveragePooling1D(),
        tf.keras.layers.Dense(2,activation='softmax')
    ])
    m.compile(optimizer='adam',
              loss='sparse_categorical_crossentropy',
              metrics=['accuracy'])
    return m

def train():
    npz = np.load(NPZ_PATH)
    X,y = npz['X'].astype(np.float32), npz['y'].astype(np.int64)
    p   = np.random.permutation(len(X)); X,y = X[p],y[p]
    split = int(0.8*len(X))
    model = build_model()
    model.fit(X[:split],y[:split],
              validation_data=(X[split:],y[split:]),
              epochs=25,batch_size=64)

    MODEL_DIR.mkdir(exist_ok=True)
    model.save(MODEL_DIR/'sitlie_fp32.h5')
    tflite = tf.lite.TFLiteConverter.from_keras_model(model).convert()
    (MODEL_DIR/'sitlie_fp32.tflite').write_bytes(tflite)
    print("[✓] exported →", MODEL_DIR/'sitlie_fp32.tflite')

# ───────────────────────────────────────── CLI
if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--prepare", action="store_true")
    ap.add_argument("--train",   action="store_true")
    args = ap.parse_args()

    if args.prepare: prepare_npz()
    if args.train  : train()
