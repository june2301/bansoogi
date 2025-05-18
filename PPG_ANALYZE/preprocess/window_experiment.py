import os
import json
import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import f_oneway
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import LabelEncoder

# --- 1) 데이터 로딩 함수 (파일당 전체 ts, sig 리턴) ---
def load_ppg_files(base_dir):
    records = []
    for subj in sorted(os.listdir(base_dir)):
        d = os.path.join(base_dir, subj)
        if not os.path.isdir(d): continue
        for fn in sorted(os.listdir(d)):
            if not fn.endswith('.json'): continue
            path = os.path.join(d, fn)
            data = json.load(open(path, 'r', encoding='utf-8'))
            ts = np.array(data['data']['ppg_continuous']['ts'])
            sig = np.array(data['data']['ppg_continuous']['green'])
            records.append({
                'subject': subj,
                'label': data['label'],
                'ts': ts,
                'sig': sig
            })
    return records

# --- 2) 전처리 & 밴드패스 ---
def butter_bandpass(sig, fs=25, low=0.5, high=5.0, order=3):
    nyq = 0.5*fs
    b,a = butter(order, [low/nyq, high/nyq], btype='band')
    return filtfilt(b,a,sig)

def preprocess_signal(ts, sig, warmup_sec=5, fs=25):
    # 예열 제거 + 선형 trend 제거 + bandpass
    start = ts[0] + warmup_sec*1000
    mask = ts >= start
    s = sig[mask]
    # detrend
    idx = np.arange(len(s))
    trend = np.polyval(np.polyfit(idx, s, 1), idx)
    s = s - trend
    s = butter_bandpass(s, fs)
    return s

# --- 3) 피처 추출 (16개) ---
def extract_features(sig, fs=25):
    import pandas as pd
    feats = {}
    # time‑domain
    feats['mean'] = sig.mean()
    feats['std']  = sig.std()
    feats['min']  = sig.min()
    feats['max']  = sig.max()
    feats['diff_std'] = np.diff(sig).std()
    feats['skew']     = pd.Series(sig).skew()
    feats['kurtosis'] = pd.Series(sig).kurtosis()
    # HRV peaks
    peaks, _ = find_peaks(sig, distance=fs*0.4)
    rr = np.diff(peaks)/fs   # seconds
    feats['n_peaks'] = len(peaks)
    feats['hr_mean'] = 60/rr.mean() if len(rr)>0 else np.nan
    feats['rr_mean'] = rr.mean()    if len(rr)>0 else np.nan
    feats['rr_std']  = rr.std()     if len(rr)>0 else np.nan
    feats['rmssd'] = np.sqrt(np.mean((np.diff(rr) ** 2))) if len(rr) > 1 else np.nan
    feats['pnn50']   = (np.abs(np.diff(rr))>0.05).sum()/len(rr) if len(rr)>1 else np.nan
    # 추가 3개
    feats['rms']  = np.sqrt((sig**2).mean())
    feats['zcr']  = ((sig[:-1]*sig[1:]<0).sum())/len(sig)
    troughs,_ = find_peaks(-sig, distance=fs*0.4)
    m = min(len(peaks), len(troughs))
    amps = np.abs(sig[peaks[:m]] - sig[troughs[:m]])
    feats['ac_amp'] = amps.mean() if amps.size>0 else np.nan
    return feats

# --- 4) 슬라이딩 윈도우 → 피처 뽑기 ---
def windows_extract(records, win_sec, overlap, fs=25):
    step = int(win_sec*(1-overlap)*fs)    # 샘플 수
    win_n = int(win_sec * fs)
    rows = []
    for rec in records:
        sig = preprocess_signal(rec['ts'], rec['sig'], warmup_sec=5, fs=fs)
        for start in range(0, len(sig)-win_n+1, step):
            seg = sig[start:start+win_n]
            feats = extract_features(seg, fs=fs)
            feats.update({'label':rec['label']})
            rows.append(feats)
    return pd.DataFrame(rows)

# --- 5) ANOVA + RF 평가 함수 ---
def eval_df(df):
    # ANOVA
    labels = df['label'].unique()
    features = [c for c in df.columns if c!='label']
    pvals = {}
    for f in features:
        groups = [df[df['label']==L][f].dropna() for L in labels]
        stat,p = f_oneway(*groups)
        pvals[f] = p
    df_anova = pd.Series(pvals).sort_values()
    # RF
    X = df[features].fillna(0)
    y = LabelEncoder().fit_transform(df['label'])
    clf = RandomForestClassifier(n_estimators=200, random_state=42)
    clf.fit(X,y)
    imps = pd.Series(clf.feature_importances_, index=features).sort_values(ascending=False)
    cv = cross_val_score(clf, X, y, cv=5)
    return df_anova, imps, cv.mean(), cv.std()

# --- 6) 메인 실행: 4×3 설정 순회 ---
if __name__=='__main__':
    base_dir = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "recordings")
    )
    records = load_ppg_files(base_dir)

    win_secs = [15, 30, 60, 120]            # 윈도우 길이 (초)
    overlaps = [0.0, 0.25, 0.5]             # 오버랩 비율
    results = []

    for w in win_secs:
        for ov in overlaps:
            df_win = windows_extract(records, win_sec=w, overlap=ov, fs=25)
            anova, imps, acc, acc_std = eval_df(df_win)
            print(f"\n=== Window={w}s, Overlap={int(ov*100)}% ===")
            print("ANOVA p-values:\n", anova.head(5))
            print("Top-5 RF importances:\n", imps.head(5))
            print(f"5‑fold CV acc: {acc:.3f} ± {acc_std:.3f}")
            results.append({
                'win': w, 'overlap': ov, 'anova': anova, 
                'importances': imps, 'acc': acc, 'acc_std': acc_std
            })

    # 필요시 결과를 pickle/CSV로 저장
    pd.DataFrame([{'win':r['win'],'ov':r['overlap'],'acc':r['acc'],'std':r['acc_std']} for r in results])\
      .to_csv("window_experiment_results.csv", index=False)
