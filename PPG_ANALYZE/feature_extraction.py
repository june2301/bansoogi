# feature_extraction.py
import os, json
import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import skew, kurtosis
from sklearn.neighbors import KNeighborsClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score

# ——— 설정 ———
FS = 25.0        # 샘플링 주파수
WIN_SEC = 10      # 윈도우 길이 (s)
OVERLAP = 0.3    # 오버랩 비율
SQI_THRESH = -5  # dB

# ——— JSON 로드 ———
def load_ppg(fp):
    with open(fp, 'r', encoding='utf-8') as f:
        d = json.load(f)
    data = d["data"]["ppg_continuous"]
    sig = np.array(data["green"], dtype=float)
    return sig

# ——— Band-pass + SQI ———
def bandpass(sig, low=0.4, high=8.0, order=4):
    nyq = 0.5 * FS
    b,a = butter(order, [low/nyq, high/nyq], btype='band')
    return filtfilt(b, a, sig)

def compute_sqi(sig):
    ac = sig - sig.mean()
    rms_sig = np.sqrt((ac**2).mean())
    b,a = butter(2, 8.0/(0.5*FS), btype='high')
    noise = filtfilt(b, a, sig)
    rms_noise = np.sqrt((noise**2).mean())
    return 20*np.log10(rms_sig/(rms_noise+1e-6))

# ——— 윈도우 분할 ———
def window_split(sig):
    n = int(WIN_SEC * FS)
    step = int(n * (1-OVERLAP))
    for i in range(0, len(sig)-n+1, step):
        yield sig[i:i+n]

# ——— 피처 추출 ———
def extract_features(w):
    feats = {
        'dc': w.mean(),
        'ac_amp': w.max()-w.min(),
        'rms': np.sqrt((w**2).mean()),
        'skewness': skew(w),
        'kurtosis': kurtosis(w),
        'zcr': ((w[:-1]*w[1:])<0).sum()/len(w)
    }
    # HR & 초단기 HRV
    peaks,_ = find_peaks(w, distance=int(0.2*FS))
    if len(peaks)>=2:
        rr = np.diff(peaks)*(1000/FS)  # ms 단위
        feats['hr']    = 60000/rr.mean()
        feats['rmssd'] = np.sqrt(np.mean(np.diff(rr)**2))
        feats['sdnn']  = np.std(rr)
    else:
        feats.update({'hr':np.nan,'rmssd':np.nan,'sdnn':np.nan})
    return feats

# ——— 파이프라인 ———
def build_df(subj):
    base = os.path.dirname(__file__)
    rec = os.path.join(base, "recordings", subj)
    records=[]
    for fn in sorted(os.listdir(rec)):
        if not fn.endswith(".json"): continue
        sig = load_ppg(os.path.join(rec, fn))
        filt = bandpass(sig)
        for w in window_split(filt):
            if compute_sqi(w) < SQI_THRESH: continue
            feats = extract_features(w)
            feats['label'] = fn.split('---')[-1].replace('.json','')
            records.append(feats)
    return pd.DataFrame(records).dropna()

# ——— 모델 평가 ———
def evaluate(df):
    X = df.drop(columns='label').values
    y = df['label'].values
    results = {}
    for name, clf in [
        ('k-NN (k=3)', KNeighborsClassifier(3)),
        ('RF (100 trees)', RandomForestClassifier(100, random_state=0))
    ]:
        scores = cross_val_score(clf, X, y, cv=5, scoring='accuracy')
        results[name] = (scores.mean(), scores.std())
    return results

# ——— 실행부 ———
if __name__ == "__main__":
    os.makedirs("outputs", exist_ok=True)
    for subj in ["subject01","subject02","subject03"]:
        print(f"\n=== {subj} ===")
        df = build_df(subj)
        print(f"Extracted windows: {len(df)}")
        res = evaluate(df)
        for name,(m,s) in res.items():
            print(f"{name}: {m:.2f} ± {s:.2f}")
        # CSV 저장
        out = f"outputs/features_{subj}.csv"
        df.to_csv(out, index=False)
        print("Saved:", out)
