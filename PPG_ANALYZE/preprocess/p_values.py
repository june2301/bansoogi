import os, json, numpy as np, pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import f_oneway
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import LeaveOneGroupOut, cross_val_score
from sklearn.inspection import permutation_importance

# === 0) 기본 설정 ===
BASE_DIR = r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings"
win_secs  = [3, 5, 10]
overlaps  = [0.0, 0.25, 0.5]
features  = ['pnn50','rr_mean','hr_mean','n_peaks','rmssd','kurtosis','skew']
labels    = ['upright-sitting','supine-lying','standing']

# === 1) 데이터 로딩 ===
def load_ppg_files(base_dir):
    records = []
    for subj in sorted(os.listdir(base_dir)):
        d = os.path.join(base_dir, subj)
        if not os.path.isdir(d): continue
        for fn in sorted(os.listdir(d)):
            if not fn.endswith('.json'): continue
            data = json.load(open(os.path.join(d, fn), 'r', encoding='utf-8'))
            records.append({
                'subject': subj,                                 # ▶ LOSO 용
                'label'  : data['label'].split('/')[-1].strip(),
                'ts'     : np.array(data['data']['ppg_continuous']['ts']),
                'sig'    : np.array(data['data']['ppg_continuous']['green'])
            })
    return records

# === 2) 전처리 + 피처 ===
def butter_bandpass(sig, fs=25, low=0.5, high=5, order=3):
    nyq = 0.5*fs
    b,a = butter(order, [low/nyq, high/nyq], btype='band')
    return filtfilt(b,a,sig)

def preprocess_signal(ts, sig, warmup_sec=5, fs=25):
    start = ts[0] + warmup_sec*1000
    s = sig[ts >= start]
    trend = np.polyval(np.polyfit(np.arange(len(s)), s, 1), np.arange(len(s)))
    return butter_bandpass(s-trend, fs)

def extract_features(sig, fs=25):
    import pandas as pd
    feats = {'pnn50':np.nan,'rr_mean':np.nan,'hr_mean':np.nan,
             'n_peaks':np.nan,'rmssd':np.nan,
             'kurtosis':pd.Series(sig).kurtosis(),
             'skew':pd.Series(sig).skew()}
    peaks,_ = find_peaks(sig, distance=fs*0.4)
    rr = np.diff(peaks)/fs
    if len(rr):
        feats.update({'n_peaks':len(peaks),
                      'rr_mean':rr.mean(),
                      'hr_mean':60/rr.mean()})
    if len(rr)>1:
        feats.update({'rmssd':np.sqrt(np.mean(np.diff(rr)**2)),
                      'pnn50':(np.abs(np.diff(rr))>0.05).mean()})
    return feats

def windows_extract(records, win_sec, overlap, fs=25):
    step  = int(win_sec*(1-overlap)*fs)
    win_n = int(win_sec*fs)
    rows  = []
    for rec in records:
        sig = preprocess_signal(rec['ts'], rec['sig'], fs=fs)
        for st in range(0, len(sig)-win_n+1, step):
            seg = sig[st:st+win_n]
            feats = extract_features(seg, fs)
            feats.update({'label':rec['label'], 'subject':rec['subject']})
            rows.append(feats)
    return pd.DataFrame(rows)

# === 3) 분석 루프 ===
records = load_ppg_files(BASE_DIR)

for w in win_secs:
    for ov in overlaps:
        df = windows_extract(records, win_sec=w, overlap=ov)
        if df.empty:
            continue
        print(f"\n=== Window={w}s, Overlap={int(ov*100)}% ===")

        # 3‑1) ANOVA
        pvals = {f: f_oneway(*[df[df.label==l][f].dropna() for l in labels])[1]
                 for f in features}
        sig_feats = [f for f,p in pvals.items() if p<0.05]
        print("Significant (p<0.05):", sig_feats)
        for f in sig_feats:
            print(f"  {f}: p={pvals[f]:.3e}")

        # 3‑2) 분포 요약
        desc = df.groupby('label')[features].agg(['mean','std'])
        for f in features:
            print(f"\n{f} 분포:")
            for l in labels:
                m,s = desc.loc[l,(f,'mean')], desc.loc[l,(f,'std')]
                print(f"  {l}: {m:.3f} ± {s:.3f}")

        # 3‑3) outlier 비율
        print("\nOutlier 비율 (mean ± 2·std):")
        for f in features:
            print(f"  {f}:")
            for l in labels:
                vals = df[df.label==l][f].dropna()
                mu,sig = vals.mean(), vals.std()
                pct = ((vals<mu-2*sig)|(vals>mu+2*sig)).mean()*100
                print(f"    {l}: {pct:.2f}%")

        # 3‑4) 효과 크기 (η²) 예시: pNN50
        groups = [df[df.label==l]["pnn50"].dropna() for l in labels]
        ss_total = df["pnn50"].var(ddof=1)*(len(df)-1)
        ss_between = sum(len(g)*(g.mean()-df["pnn50"].mean())**2 for g in groups)
        eta2 = ss_between/ss_total if ss_total else np.nan
        print(f"\npNN50 η² = {eta2:.3f}")

        # 3‑5) LOSO‑CV Accuracy
        X, y   = df[features].fillna(0), df['label']
        groups = df['subject']
        clf    = RandomForestClassifier(n_estimators=200, random_state=42)
        acc    = cross_val_score(clf, X, y, groups=groups,
                                 cv=LeaveOneGroupOut()).mean()
        print(f"LOSO‑CV accuracy = {acc:.3f}")

        # 3‑6) Permutation Importance 안정성
        clf.fit(X, y)
        perm = permutation_importance(clf, X, y, n_repeats=30,
                                      random_state=0, n_jobs=-1)
        print("Perm. importance STD (평균 ±): "
              f"{perm.importances_std.mean():.4f} ± {perm.importances_std.std():.4f}")

        print("-"*70)
