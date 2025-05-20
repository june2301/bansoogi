#!/usr/bin/env python
# train_ppg_model.py
# pip install numpy pandas scipy scikit-learn tensorflow==2.15.0 lightgbm
import os, json, argparse, pathlib, warnings
import numpy as np, pandas as pd
from scipy.signal import butter, filtfilt, find_peaks, welch
from sklearn.model_selection import LeaveOneGroupOut
from tensorflow.keras import layers, models
import tensorflow as tf

warnings.filterwarnings("ignore")

# ─────────────────────────────── 설정 ───────────────────────────────
BASE_DIR   = pathlib.Path("recordings")                # json 모음 폴더
WIN_LIST   = [5, 10, 15]                               # 초
OVL_LIST   = [0.0, 0.25, 0.5]
FS         = 25                                        # Hz
LABELS     = ['upright-sitting', 'supine-lying', 'standing']
FEATS_10   = ['pnn50','rr_mean','hr_mean','rmssd','n_peaks',
              'crest_t','dwell_t','pwtf','kurtosis','skew' ]

# ───────────────────── ① 공통 전처리 & 피처 ──────────────────────
def _band(x, fs=25, lo=0.5, hi=5, order=3):
    nyq = 0.5*fs
    b, a = butter(order, [lo/nyq, hi/nyq], btype='band')
    return filtfilt(b, a, x)

def _preprocess(ts, sig):
    sig = sig[ts >= ts[0] + 5_000]                      # 5 s 예열 제거
    trend = np.polyval(np.polyfit(np.arange(len(sig)), sig, 1),
                       np.arange(len(sig)))
    return _band(sig - trend, FS)

def _ppg_feats(x):
    out = {k: np.nan for k in FEATS_10}
    # PPG peak / trough
    pk, _ = find_peaks(x, distance=FS*0.4)
    tr, _ = find_peaks(-x, distance=FS*0.4)
    rr = np.diff(pk)/FS
    if len(rr):
        out.update({'n_peaks': len(pk), 'rr_mean': rr.mean(),
                    'hr_mean': 60/rr.mean()})
    if len(rr) > 1:
        out.update({'rmssd': np.sqrt(np.mean(np.diff(rr)**2)),
                    'pnn50': (np.abs(np.diff(rr)) > 0.05).mean()})
    # crest / dwell / pwtf
    cyc = []
    for p in pk:
        f = tr[tr < p]
        n = tr[tr > p]
        if len(f)==0 or len(n)==0: continue
        cyc.append((f[-1], p, n[0]))
    if cyc:
        ct  = np.mean([(p-f)/FS for f,p,_ in cyc])
        dw  = np.mean([(n-f)/FS for f,_,n in cyc])
        out.update({'crest_t': ct, 'dwell_t': dw, 'pwtf': ct/dw if dw else np.nan})
    # 왜도·첨도
    out['skew']     = pd.Series(x).skew()
    out['kurtosis'] = pd.Series(x).kurtosis()
    return out

def _sliding(sig, win_s, ovl):
    step = int(win_s * (1-ovl) * FS)
    n    = int(win_s * FS)
    for s in range(0, len(sig)-n+1, step):
        yield sig[s:s+n]

def load_dataset():
    rows = []
    for subj in sorted(os.listdir(BASE_DIR)):
        for f in (BASE_DIR/subj).glob("*.json"):
            d   = json.load(open(f, encoding='utf‑8'))
            lab = d['label'].split('/')[-1].strip()
            ts  = np.asarray(d['data']['ppg_continuous']['ts'])
            sg  = np.asarray(d['data']['ppg_continuous']['green'])
            sg  = _preprocess(ts, sg)
            for W in WIN_LIST:
                for OV in OVL_LIST:
                    for seg in _sliding(sg, W, OV):
                        feat = _ppg_feats(seg)
                        feat.update({'label': lab, 'subject': subj,
                                     'win': W, 'ovl': OV})
                        rows.append(feat)
    df = pd.DataFrame(rows)
    print("Raw DF :", df.shape)
    return df.dropna(subset=['label'])

# ─────────────── ② 모델 학습 (윈도우·오버랩 별) ────────────────
def train_and_export(df, win, ovl, out_dir="models"):
    sub = df.query("win==@win and ovl==@ovl").copy()
    X   = sub[FEATS_10].fillna(0).to_numpy(dtype='float32')
    y   = sub['label'].map({l:i for i,l in enumerate(LABELS)}).to_numpy()
    g   = sub['subject'].to_numpy()

    # 간단한 3‑dense MLP  (feature 10개 → 64 → 32 → 3class)
    model = models.Sequential([
        layers.Input(shape=(10,)),
        layers.Dense(64, activation='relu'),
        layers.Dense(32, activation='relu'),
        layers.Dense(3,  activation='softmax')])

    model.compile(optimizer='adam',
                  loss='sparse_categorical_crossentropy',
                  metrics=['accuracy'])

    # Subject LOSO CV
    logo = LeaveOneGroupOut()
    accs = []
    for tr, te in logo.split(X, y, g):
        mdl = tf.keras.models.clone_model(model)
        mdl.compile(optimizer='adam',
                    loss='sparse_categorical_crossentropy',
                    metrics=['accuracy'])
        mdl.fit(X[tr], y[tr], epochs=40, verbose=0, batch_size=32)
        accs.append(mdl.evaluate(X[te], y[te], verbose=0)[1])
    print(f"[{win}s/{int(ovl*100)}%]  LOSO‑CV  acc={np.mean(accs):.3f}")

    # full‑fit & export
    model.fit(X, y, epochs=40, verbose=0, batch_size=32)
    pathlib.Path(out_dir).mkdir(exist_ok=True)
    saved = pathlib.Path(out_dir)/f"ppg_{win}s_{int(ovl*100)}.keras"
    model.save(saved)

    # TFLite 변환
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    # converter.optimizations = [tf.lite.Optimize.DEFAULT]   # size + int8 양자화
    tflite_model = converter.convert()
    open(saved.with_suffix(".tflite"), "wb").write(tflite_model)

# ───────────────────────────── main ───────────────────────────────
if __name__ == "__main__":
    df = load_dataset()
    for W in WIN_LIST:
        for OV in OVL_LIST:
            train_and_export(df, W, OV)
    print("✓ 모든 TFLite 모델이 models/ 폴더에 저장되었습니다.")
