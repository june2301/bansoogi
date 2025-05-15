import os, json
import numpy as np
import pandas as pd
from scipy.signal import find_peaks
from sklearn.model_selection import cross_val_score
from sklearn.neighbors import KNeighborsClassifier

# 1. 파일 스캔 & 필터링
def list_valid_files(folder, min_size=10*1024):
    return [
        os.path.join(folder, f)
        for f in os.listdir(folder)
        if f.endswith(".json") and os.path.getsize(os.path.join(folder, f)) > min_size
    ]

# 2. JSON 로드
def load_ppg(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        d = json.load(f)
    data = d["data"]["ppg_continuous"]
    ts = np.array(d["start_ts"] + np.array(data["ts"]))  # absolute ms
    # pick green channel for simplicity
    sig = np.array(data["green"])
    label = file_path.split('_')[-1].replace('.json','')  # 파일명 끝부분이 라벨
    return ts, sig, label

# 3. 샘플링 확인
def check_sampling(ts):
    diffs = np.diff(ts)
    mean_dt = np.mean(diffs)
    print(f"평균 샘플 간격: {mean_dt:.1f} ms  (~{1000/mean_dt:.1f} Hz)")

# 4. 윈도우 분할
def window_data(ts, sig, window_sec=10, overlap=0.3):
    print(f'윈도우 {window_sec}초, 오버랩 {overlap}')
    fs = 1000 / np.mean(np.diff(ts))  # Hz
    win_n = int(window_sec * fs)
    step = int(win_n * (1-overlap))
    windows = []
    for start in range(0, len(sig)-win_n+1, step):
        w_ts = ts[start:start+win_n]
        w_sig = sig[start:start+win_n]
        windows.append((w_ts, w_sig))
    return windows

# 5. 피처 추출
def extract_features(w_ts, w_sig):
    features = {}
    features['dc'] = np.mean(w_sig)
    features['ac_amp'] = np.max(w_sig) - np.min(w_sig)
    features['rms'] = np.sqrt(np.mean(w_sig**2))
    # simple HR estimation via peak-detection
    # find peaks at least 200ms apart
    peaks, _ = find_peaks(w_sig, distance= int(0.2*(1000/np.mean(np.diff(w_ts)))))
    # RR intervals (ms)
    if len(peaks)>=2:
        rr = np.diff(w_ts[peaks])
        bpm = 60000 / np.mean(rr)
    else:
        bpm = np.nan
    features['hr'] = bpm
    return features

# 6. 전체 파이프라인
def build_feature_df(folder):
    records = []
    for fn in list_valid_files(folder):
        ts, sig, label = load_ppg(fn)
        check_sampling(ts)
        for w_ts, w_sig in window_data(ts, sig):
            feats = extract_features(w_ts, w_sig)
            feats['label'] = label
            records.append(feats)
    return pd.DataFrame(records).dropna()

# 7. 간단 분류 테스트
def quick_classify(df):
    X = df[['dc','ac_amp','rms','hr']].values
    y = df['label'].values
    clf = KNeighborsClassifier(n_neighbors=3)
    scores = cross_val_score(clf, X, y, cv=5, scoring='accuracy')
    print(f"k-NN 5-fold accuracy: {np.mean(scores):.2f} ± {np.std(scores):.2f}")

if __name__ == "__main__":
    folder = "./recordings/subject03"
    df = build_feature_df(folder)
    print("추출된 윈도우 수:", len(df))
    quick_classify(df)
