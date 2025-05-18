import os
import json
import numpy as np
import pandas as pd
import matplotlib
matplotlib.rcParams['font.family'] = 'Malgun Gothic'
matplotlib.rcParams['axes.unicode_minus'] = False
import matplotlib.pyplot as plt
from scipy.signal import butter, filtfilt, find_peaks

# === 환경 설정 ===
BASE_DIR = r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings"
OUTPUT_DIR = "results/plots/distributions"
os.makedirs(OUTPUT_DIR, exist_ok=True)
print("BASE_DIR:", BASE_DIR)
print("폴더 내용:", os.listdir(BASE_DIR))

# --- 기존 함수들 재사용 ---
def butter_bandpass(sig, fs=25, low=0.5, high=5.0, order=3):
    nyq = 0.5 * fs
    b, a = butter(order, [low/nyq, high/nyq], btype='band')
    return filtfilt(b, a, sig)

def preprocess_signal(ts, sig, warmup_sec=5, fs=25):
    start = ts[0] + warmup_sec*1000
    mask = ts >= start
    s = sig[mask]
    idx = np.arange(len(s))
    trend = np.polyval(np.polyfit(idx, s, 1), idx)
    s = s - trend
    return butter_bandpass(s, fs)

def extract_features(sig, fs=25):
    import pandas as pd
    feats = {
        'pnn50': None, 'rr_mean': None, 'hr_mean': None,
        'n_peaks': None, 'rmssd': None, 'kurtosis': None, 'skew': None
    }
    # 시간 도메인 분포
    feats['kurtosis'] = pd.Series(sig).kurtosis()
    feats['skew']     = pd.Series(sig).skew()
    # HRV 기반
    peaks, _ = find_peaks(sig, distance=fs*0.4)
    rr = np.diff(peaks) / fs
    feats['n_peaks'] = len(peaks)
    feats['rr_mean'] = rr.mean() if len(rr)>0 else np.nan
    feats['hr_mean'] = 60/rr.mean() if len(rr)>0 else np.nan
    feats['rmssd']   = np.sqrt(np.mean((np.diff(rr)**2))) if len(rr)>1 else np.nan
    feats['pnn50']   = (np.abs(np.diff(rr))>0.05).sum()/len(rr) if len(rr)>1 else np.nan
    return feats

def load_ppg_files(base_dir):
    recs = []
    for subj in sorted(os.listdir(base_dir)):
        d = os.path.join(base_dir, subj)
        if not os.path.isdir(d): continue
        for fn in sorted(os.listdir(d)):
            if fn.endswith('.json'):
                data = json.load(open(os.path.join(d, fn), 'r', encoding='utf-8'))
                raw_label = data['label']                        # ex. "바른 자세로 앉기 / upright-sitting"
                eng_label = raw_label.split('/')[-1].strip()     # -> "upright-sitting"
                ts  = np.array(data['data']['ppg_continuous']['ts'])
                sig = np.array(data['data']['ppg_continuous']['green'])
                recs.append({'subject': subj, 'label': eng_label, 'ts': ts, 'sig': sig})
    return recs

def windows_extract(records, win_sec, overlap, fs=25):
    step = int(win_sec * (1-overlap) * fs)
    win_n = int(win_sec * fs)
    rows = []
    for rec in records:
        sig = preprocess_signal(rec['ts'], rec['sig'], warmup_sec=5, fs=fs)
        for start in range(0, len(sig)-win_n+1, step):
            seg = sig[start:start+win_n]
            feats = extract_features(seg, fs)
            feats['label'] = rec['label']
            rows.append(feats)
    return pd.DataFrame(rows)

# === 실행 파이프라인 ===
records = load_ppg_files(BASE_DIR)
print("로딩된 레코드 수:", len(records))
if not records:
    raise RuntimeError("레코드가 하나도 로드되지 않았습니다. BASE_DIR 경로를 확인하세요.")

win_secs = [15, 30, 60, 120]
overlaps = [0.0, 0.25, 0.5]
features = ['pnn50', 'rr_mean', 'hr_mean', 'n_peaks', 'rmssd', 'kurtosis', 'skew']
label_map = {'upright-sitting': '앉음', 'supine-lying': '눕음', 'standing': '섬'}

for w in win_secs:
    for ov in overlaps:
        df_win = windows_extract(records, win_sec=w, overlap=ov)
        print("윈도우 DF shape:", df_win.shape)
        print(df_win[['label','hr_mean']].head())
        for feat in features:
            data = [ df_win[df_win['label']==lbl][feat].dropna() 
                     for lbl in label_map.keys() ]
            if not any(len(d)>0 for d in data):
                print(f"{feat} 에 대한 데이터가 없습니다.")
                continue
            plt.figure(figsize=(6,4))
            plt.boxplot(data, tick_labels=[label_map[l] for l in label_map.keys()], showfliers=True)
            plt.title(f"{feat} 분포 (win={w}s, ov={int(ov*100)}%)")
            plt.xlabel("자세")
            plt.ylabel(feat)
            plt.grid(axis='y', linestyle='--', alpha=0.5)
            plt.tight_layout()
            plt.savefig(os.path.join(OUTPUT_DIR, f"{feat}_win{w}_ov{int(ov*100)}.png"))
            plt.close()
