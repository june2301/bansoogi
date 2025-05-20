import os
import json
import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import f_oneway
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import LabelEncoder

# 1) Load records
def load_ppg_files(base_dir):
    records = []
    for subj in sorted(os.listdir(base_dir)):
        subj_dir = os.path.join(base_dir, subj)
        if not os.path.isdir(subj_dir):
            continue
        for fname in sorted(os.listdir(subj_dir)):
            if not fname.endswith('.json'):
                continue
            data = json.load(open(os.path.join(subj_dir, fname), 'r', encoding='utf-8'))
            rec = {
                'subject': subj,
                'label': data['label'],
                'ts': np.array(data['data']['ppg_continuous']['ts']),
                'green': np.array(data['data']['ppg_continuous']['green'])
            }
            records.append(rec)
    return records

# 2) Filter
def butter_bandpass_filter(sig, fs, lowcut=0.5, highcut=5.0, order=3):
    nyq = 0.5 * fs
    b, a = butter(order, [lowcut/nyq, highcut/nyq], btype='band')
    return filtfilt(b, a, sig)

# 3) Preprocess
def preprocess_signal(ts, sig, warmup_sec=5, fs=25, apply_filter=True):
    start_time = ts[0] + warmup_sec * 1000
    mask = ts >= start_time
    sig = sig[mask]
    # detrend
    sig = sig - np.polyval(np.polyfit(np.arange(len(sig)), sig, 1), np.arange(len(sig)))
    if apply_filter:
        sig = butter_bandpass_filter(sig, fs)
    return sig

# 4) Feature extraction with additional features
def extract_features(sig, fs=25):
    feats = {}
    # existing time-domain
    feats['mean'] = np.mean(sig)
    feats['std'] = np.std(sig)
    feats['min'] = np.min(sig)
    feats['max'] = np.max(sig)
    feats['diff_std'] = np.std(np.diff(sig))
    feats['skew'] = pd.Series(sig).skew()
    feats['kurtosis'] = pd.Series(sig).kurtosis()
    # HRV peaks
    peaks, _ = find_peaks(sig, distance=fs*0.4)
    rr = np.diff(peaks) / fs  # in seconds
    feats['n_peaks'] = len(peaks)
    feats['hr_mean'] = 60.0/np.mean(rr) if len(rr)>0 else np.nan
    feats['rr_mean'] = np.mean(rr) if len(rr)>0 else np.nan
    feats['rr_std'] = np.std(rr) if len(rr)>0 else np.nan
    feats['rmssd'] = np.sqrt(np.mean(np.diff(rr)**2)) if len(rr)>1 else np.nan
    feats['pnn50'] = np.sum(np.abs(np.diff(rr))>0.05)/len(rr) if len(rr)>1 else np.nan
    
    # additional 3 features
    # RMS
    feats['rms'] = np.sqrt(np.mean(sig**2))
    # Zero-crossing rate
    feats['zcr'] = np.mean((sig[:-1]*sig[1:] < 0).astype(int))
    # AC amplitude: mean peak-to-trough
    troughs, _ = find_peaks(-sig, distance=fs*0.4)
    # match peaks and troughs one-to-one up to min length
    n = min(len(peaks), len(troughs))
    amps = np.abs(sig[peaks[:n]] - sig[troughs[:n]])
    feats['ac_amp'] = np.mean(amps) if amps.size>0 else np.nan
    
    return feats

# 5) Build DataFrame
base_dir = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "recordings")
    )
records = load_ppg_files(base_dir)
rows = []
for rec in records:
    sig = preprocess_signal(rec['ts'], rec['green'], warmup_sec=5, apply_filter=True)
    feats = extract_features(sig)
    feats.update({'subject': rec['subject'], 'label': rec['label']})
    rows.append(feats)
df = pd.DataFrame(rows)

# 6) ANOVA
labels = df['label'].unique()
feature_cols = [c for c in df.columns if c not in ['subject','label']]
anova_p = {}
for feat in feature_cols:
    groups = [df[df['label']==lbl][feat].dropna() for lbl in labels]
    stat, p = f_oneway(*groups)
    anova_p[feat] = p
df_anova = pd.Series(anova_p, name='p_value').sort_values()
print("=== ANOVA p-values ===")
print(df_anova)

# 7) Random Forest
le = LabelEncoder().fit(df['label'])
X = df[feature_cols].fillna(0)
y = le.transform(df['label'])
clf = RandomForestClassifier(n_estimators=200, random_state=42)
clf.fit(X, y)
importances = pd.Series(clf.feature_importances_, index=feature_cols).sort_values(ascending=False)
print("\n=== Feature importances ===")
print(importances)

# 8) Cross-validation
cv_scores = cross_val_score(clf, X, y, cv=5)
print(f"\n5-fold CV accuracy: {cv_scores.mean():.3f} ± {cv_scores.std():.3f}")
