import os
import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import f_oneway
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import LabelEncoder

# === 설정: recordings 폴더 경로를 본인 환경에 맞게 수정 ===
BASE_DIR = r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings"
OUTPUT_DATA_DIR = "results/data"
OUTPUT_PLOT_DIR = "results/plots/anova"

os.makedirs(OUTPUT_DATA_DIR, exist_ok=True)
os.makedirs(OUTPUT_PLOT_DIR, exist_ok=True)

# --- 1) 데이터 로딩 ---
def load_ppg_files(base_dir):
    records = []
    for subj in sorted(os.listdir(base_dir)):
        subj_dir = os.path.join(base_dir, subj)
        if not os.path.isdir(subj_dir):
            continue
        for fn in sorted(os.listdir(subj_dir)):
            if not fn.endswith('.json'):
                continue
            path = os.path.join(subj_dir, fn)
            data = json.load(open(path, 'r', encoding='utf-8'))
            ts = np.array(data['data']['ppg_continuous']['ts'])
            sig = np.array(data['data']['ppg_continuous']['green'])
            records.append({'subject': subj, 'label': data['label'], 'ts': ts, 'sig': sig})
    return records

# --- 2) 전처리 & Band‑pass ---
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

# --- 3) 피처 추출 (총 16개) ---
def extract_features(sig, fs=25):
    import pandas as pd
    feats = {
        'mean': sig.mean(),
        'std': sig.std(),
        'min': sig.min(),
        'max': sig.max(),
        'diff_std': np.diff(sig).std(),
        'skew': pd.Series(sig).skew(),
        'kurtosis': pd.Series(sig).kurtosis()
    }
    peaks, _ = find_peaks(sig, distance=fs*0.4)
    rr = np.diff(peaks) / fs
    feats.update({
        'n_peaks': len(peaks),
        'hr_mean': 60/rr.mean() if len(rr)>0 else np.nan,
        'rr_mean': rr.mean() if len(rr)>0 else np.nan,
        'rr_std': rr.std() if len(rr)>0 else np.nan,
        'rmssd': np.sqrt(np.mean((np.diff(rr)**2))) if len(rr)>1 else np.nan,
        'pnn50': (np.abs(np.diff(rr))>0.05).sum()/len(rr) if len(rr)>1 else np.nan,
        'rms': np.sqrt((sig**2).mean()),
        'zcr': ((sig[:-1]*sig[1:]<0).sum())/len(sig)
    })
    troughs, _ = find_peaks(-sig, distance=fs*0.4)
    m = min(len(peaks), len(troughs))
    amps = np.abs(sig[peaks[:m]] - sig[troughs[:m]])
    feats['ac_amp'] = amps.mean() if amps.size > 0 else np.nan
    return feats

# --- 4) 슬라이딩 윈도우로 피처 뽑기 ---
def windows_extract(records, win_sec, overlap, fs=25):
    step = int(win_sec * (1-overlap) * fs)
    win_n = int(win_sec * fs)
    rows = []
    for rec in records:
        sig = preprocess_signal(rec['ts'], rec['sig'], warmup_sec=5, fs=fs)
        for start in range(0, len(sig) - win_n + 1, step):
            seg = sig[start:start+win_n]
            feats = extract_features(seg, fs)
            feats['label'] = rec['label']
            rows.append(feats)
    return pd.DataFrame(rows)

# --- 5) ANOVA + RF 평가 및 저장 ---
def eval_df(df, win, ov):
    labels = df['label'].unique()
    features = [c for c in df.columns if c != 'label']
    
    # ANOVA
    pvals = {}
    for f in features:
        groups = [df[df['label']==L][f].dropna() for L in labels]
        _, p = f_oneway(*groups)
        pvals[f] = p
    df_anova = pd.Series(pvals).sort_values()
    df_anova.to_csv(os.path.join(OUTPUT_DATA_DIR, f"anova_win{win}_ov{int(ov*100)}.csv"), header=['p_value'])
    
    # ANOVA 그래프
    plt.figure(figsize=(8,4))
    ax = df_anova.plot(kind='bar', logy=True)
    ax.axhline(0.05, color='red', linestyle='--', label='p=0.05')
    ax.set_title(f"ANOVA p-values (win={win}s, ov={int(ov*100)}%)")
    ax.set_ylabel("p-value (log scale)")
    plt.xticks(rotation=45, ha='right')
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(OUTPUT_PLOT_DIR, f"anova_win{win}_ov{int(ov*100)}.png"))
    plt.close()
    
    # Random Forest feature importance (Series로 생성)
    X = df[features].fillna(0)
    y = LabelEncoder().fit_transform(df['label'])
    clf = RandomForestClassifier(n_estimators=200, random_state=42)
    clf.fit(X, y)
    imps = pd.Series(clf.feature_importances_, index=features).sort_values(ascending=False)
    
    # Cross-validation
    cv_scores = cross_val_score(clf, X, y, cv=5)
    
    return df_anova, imps, cv_scores.mean(), cv_scores.std()

# --- 6) 메인 실행 ---
if __name__ == '__main__':
    records = load_ppg_files(BASE_DIR)
    win_secs = [15, 30, 60, 120]
    overlaps = [0.0, 0.25, 0.5]
    summary = []
    
    for w in win_secs:
        for ov in overlaps:
            df_win = windows_extract(records, win_sec=w, overlap=ov)
            _, imps, acc, acc_std = eval_df(df_win, w, ov)
            summary.append({
                'win': w,
                'overlap': ov,
                'accuracy': acc,
                'acc_std': acc_std,
                # top-3 importances 저장
                'top1': imps[0], 'feat1': imps.index[0],
                'top2': imps[1], 'feat2': imps.index[1],
                'top3': imps[2], 'feat3': imps.index[2]
            })
            print(f"win={w}s ov={int(ov*100)}% -> acc={acc:.3f} ± {acc_std:.3f}")
    
    # 설정별 요약 저장
    pd.DataFrame(summary).to_csv(
        os.path.join(OUTPUT_DATA_DIR, "window_summary.csv"),
        index=False
    )
