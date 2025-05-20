#!/usr/bin/env python
# train_ppg_model_with_calib_mod_refactored.py – 리팩토링된 학습 스크립트
# ------------------------------------------------
# · HRV‑10 전처리 로직 유지
# · raw green 신호 μ/σ 재계산하여 stats_raw.green 갱신
# · feature별 quantile 임계치(5–95%) 및 분포(stats) 계산 후 calib.json에 저장
# · 학습 시에는 "train 분포" 기반 Z‑score 정규화된 값으로 학습
# · LOSO‑CV + 최종 TFLite 저장
# ------------------------------------------------
import json
import pathlib
import warnings

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import LabelEncoder
from tensorflow.keras import layers, models
import tensorflow as tf

warnings.filterwarnings("ignore")

# ─────────────────────────── 설정 ───────────────────────────
BASE_DIR = pathlib.Path("recordings")   # JSON recordings 폴더 루트
WIN_S    = 10                            # 윈도우 길이 (초)
OVL      = 0.50                          # 윈도우 오버랩 비율
FS       = 25                            # 샘플링 주파수 (Hz)
LABELS   = ['upright-sitting', 'supine-lying', 'standing']
FEATURES = [  # HRV‑10
    'pnn50','rr_mean','hr_mean','rmssd','n_peaks',
    'crest_t','dwell_t','pwtf','kurtosis','skew'
]

# ────────────────── 신호 전처리 헬퍼 ──────────────────
def detrend(x: np.ndarray) -> np.ndarray:
    idx = np.arange(x.size, dtype=float)
    coef = np.polyfit(idx, x, 1)
    return x - np.polyval(coef, idx)


def bandpass(x: np.ndarray, lo=0.5, hi=5.0, fs=FS, order=2) -> np.ndarray:
    nyq = fs * 0.5
    b, a = butter(order, [lo/nyq, hi/nyq], btype='band')
    return filtfilt(b, a, filtfilt(b, a, x))


def extract_features(seg: np.ndarray, mu_raw: float, sigma_raw: float) -> np.ndarray:
    # 1) raw → 사용자 기준 Z‑score 정규화
    normed = (seg - mu_raw) / sigma_raw
    # 2) detrend & bandpass
    x = bandpass(detrend(normed))

    # 3) HRV‑10 피처 추출
    min_dist = int(FS * 0.4)
    peaks, _   = find_peaks(x, distance=min_dist)
    troughs, _ = find_peaks(-x, distance=min_dist)
    rr = np.diff(peaks) / FS if peaks.size > 1 else np.array([])

    f = np.zeros(len(FEATURES), dtype=np.float32)
    f[FEATURES.index('n_peaks')] = peaks.size

    if rr.size:
        f[FEATURES.index('rr_mean')] = rr.mean()
        f[FEATURES.index('hr_mean')] = 60. / rr.mean()
    if rr.size > 1:
        diff = np.diff(rr)
        f[FEATURES.index('rmssd')] = np.sqrt(np.mean(diff**2))
        f[FEATURES.index('pnn50')] = np.mean(np.abs(diff) > 0.05)

    triples = []
    for p in peaks:
        left  = troughs[troughs < p]
        right = troughs[troughs > p]
        if left.size and right.size:
            triples.append((left.max(), p, right.min()))
    if triples:
        ct = np.mean([(p - f_) / FS for f_, p, _ in triples])
        dw = np.mean([(n - f_) / FS for f_, _, n in triples])
        f[FEATURES.index('crest_t')] = ct
        f[FEATURES.index('dwell_t')] = dw
        f[FEATURES.index('pwtf')]  = ct / dw if dw else 0.

    f[FEATURES.index('kurtosis')] = pd.Series(x).kurtosis()
    f[FEATURES.index('skew')]     = pd.Series(x).skew()

    return f


def sliding_windows(sig: np.ndarray):
    length = WIN_S * FS
    step   = int(length * (1 - OVL))
    for start in range(0, len(sig) - length + 1, step):
        yield sig[start: start + length]

# ─────────────────── 데이터 로딩 ───────────────────
def load_dataset(mu_raw: float, sigma_raw: float):
    rows, labels, groups = [], [], []
    raw_concat = []
    for subj in sorted(BASE_DIR.iterdir()):
        if not subj.is_dir(): continue
        for fn in sorted(subj.glob('*.json')):
            d = json.loads(fn.read_text(encoding='utf-8'))
            label = d['label'].split('/ ')[-1]
            raw   = np.array(
                d['data']['ppg_continuous']['green'],
                dtype=float
            ) / 4096.
            raw_concat.append(raw)
            sig = raw[int(FS*5):]  # 5초 웜업 컷
            for seg in sliding_windows(sig):
                rows.append(extract_features(seg, mu_raw, sigma_raw))
                labels.append(label)
                groups.append(subj.name)

    df = pd.DataFrame(rows, columns=FEATURES)
    df['label'] = labels
    # print(df['label'].value_counts())
    df['group'] = groups
    # 문자열 정규화
        # 1) 모든 대시 문자를 ASCII '-' 로 통일
    df['label'] = (
        df['label']
        .str.replace(r'[‐‑‒–—−]', '-', regex=True)
        .str.strip()
    )

    # 2) 중복된 라벨을 한 번에 매핑
    df['label'] = df['label'].replace({
        'upright-sitting': 'upright-sitting',  # (실제 두 형태가 동일하니 예시)
        'upright‑sitting': 'upright-sitting',
        'supine-lying':    'supine-lying',
        'supine‑lying':    'supine-lying',
    })
    return df, np.concatenate(raw_concat)

# ─────────────────── 메인 ───────────────────
if __name__ == '__main__':
    calib_fp = pathlib.Path('models/train_calib.json')
    calib_fp.parent.mkdir(parents=True, exist_ok=True)

    # 0) 이전 calib 로드 (stats_raw, stats, clip_bounds)
    if calib_fp.exists():
        calib = json.loads(calib_fp.read_text())
    else:
        calib = {'stats_raw': {}, 'stats': {}, 'clip_bounds': {}}

    mu_raw    = calib.get('stats_raw', {}).get('green', {}).get('mu', 0.0)
    sigma_raw = calib.get('stats_raw', {}).get('green', {}).get('sigma', 1.0)

    # 1) 데이터 + 피처 추출
    df, raw_all = load_dataset(mu_raw, sigma_raw)
    print("Dataset windows:", df.shape)

    # 2) raw μ/σ 재계산
    mu_new    = raw_all.mean()
    sigma_new = raw_all.std(ddof=0)
    calib['stats_raw'] = {'green': {'mu': float(mu_new), 'sigma': float(sigma_new)}}

    # 3) feature별 stats & clip 계산 → JSON 업데이트
    stats, clip = {}, {}
    for f in FEATURES:
        s = df[f].astype(float)
        lo, hi = s.quantile(0.05), s.quantile(0.95)
        clip[f] = [float(lo), float(hi)]
        sc = s.clip(lo, hi)
        stats[f] = {'mu': float(sc.mean()), 'sigma': float(sc.std(ddof=0))}
    calib['stats']       = stats
    calib['clip_bounds'] = clip
    calib_fp.write_text(json.dumps(calib, indent=2))
    print('calib.json updated')

    # ── 여기에 추가 ──
    # 3.5) 실제 학습 데이터에도 클리핑 적용
    # for f in FEATURES:
    #     lo, hi = calib['clip_bounds'][f]
    #     df[f] = df[f].clip(lower=lo, upper=hi)
    # print('Applied train clip_bounds to df')

    # 4) 학습 데이터 준비: 통계 기반 Z‑score 정규화 적용
    # → inference 시와 동일한 분포를 모델에 학습시키기 위함
    # df_raw = 원본 피처
    X_raw     = df[FEATURES].values.astype(np.float32)
    # mu_train  = np.array([stats[f]['mu'] for f in FEATURES], dtype=np.float32)
    # sigma_train = np.array([stats[f]['sigma'] for f in FEATURES], dtype=np.float32)
    # Z‑score 정규화
    # X = (X_raw - mu_train) / sigma_train
    X = X_raw

    # 라벨 & 그룹
    mapping = {l: i for i, l in enumerate(LABELS)}
    y = df['label'].map(mapping).fillna(0).astype(np.int32)
    g = LabelEncoder().fit_transform(df['group'])
    print(df['label'].value_counts())
    # 5) MLP + LOSO‑CV
    logo = LeaveOneGroupOut()
    accs = []
    for tr, te in logo.split(X, y, g):
        model = models.Sequential([
            layers.Input(X.shape[1]),
            layers.Dense(128, 'relu'), layers.Dropout(0.3),
            layers.Dense(64,  'relu'), layers.Dropout(0.3),
            layers.Dense(len(LABELS), 'softmax')
        ])
        model.compile('adam', 'sparse_categorical_crossentropy', ['accuracy'])
        model.fit(
            X[tr], y[tr], epochs=40, batch_size=32,
            verbose=0,
            class_weight={0:1.0, 1:1.0, 2:1.0}
        )
        accs.append(model.evaluate(X[te], y[te], verbose=0)[1])
    print(f"MLP LOSO: {np.mean(accs):.3f} ± {np.std(accs):.3f}")

    # 6) 최종 재학습 + 저장
    final = models.clone_model(model)
    final.compile('adam', 'sparse_categorical_crossentropy', ['accuracy'])
    final.fit(X, y, epochs=40, batch_size=32, verbose=0, class_weight={0:1.0,1:1.0,2:1.0})
    out = pathlib.Path('models'); out.mkdir(exist_ok=True)
    final.save(out/'ppg_model.keras')
    tflite = tf.lite.TFLiteConverter.from_keras_model(final).convert()
    (out/'ppg_model.tflite').write_bytes(tflite)
    print('모델 저장 완료')
