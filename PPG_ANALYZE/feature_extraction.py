import os, json
import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from sklearn.neighbors import KNeighborsClassifier
from sklearn.model_selection import cross_val_score

# ——— 설정 ———
FS = 25.0          # PPG 샘플링 주파수 [Hz]
WIN_SEC = 7        # 윈도우 길이 [s]
OVERLAP = 0.3      # 오버랩 비율 [0–1]
SQI_THRESH = -10.0   # 최소 SNR 임계치 [dB] (예시)

# ——— 1) JSON 로드 ———
def load_ppg(fp):
    with open(fp, 'r', encoding='utf-8') as f:
        d = json.load(f)
    data = d["data"]["ppg_continuous"]
    ts = np.array(data["ts"], dtype=float)
    sig = np.array(data["green"], dtype=float)
    return ts, sig

# ——— 2) 밴드패스 & SQI ———
def bandpass(sig, low=0.4, high=8.0, order=4):
    nyq = 0.5 * FS
    b,a = butter(order, [low/nyq, high/nyq], btype='band')
    return filtfilt(b, a, sig)

def compute_sqi(sig):
    ac = sig - sig.mean()
    rms_sig = np.sqrt((ac**2).mean())
    # 노이즈: high-pass >8Hz
    b,a = butter(2, 8.0/(0.5*FS), btype='high')
    noise = filtfilt(b, a, sig)
    rms_noise = np.sqrt((noise**2).mean())
    return 20*np.log10(rms_sig / (rms_noise+1e-6))

# ——— 3) 윈도우 분할 ———
def window_split(sig):
    n_win = int(WIN_SEC * FS)
    step = int(n_win * (1-OVERLAP))
    for start in range(0, len(sig)-n_win+1, step):
        yield sig[start:start+n_win]

# ——— 4) 피처 추출 ———
def extract_features(w):
    feats = {}
    feats['dc']     = w.mean()
    feats['ac_amp'] = w.max() - w.min()
    feats['rms']    = np.sqrt((w**2).mean())
    # peak 기반 HR
    peaks,_ = find_peaks(w, distance=int(0.2*FS))
    if len(peaks)>=2:
        rr = np.diff(peaks)*(1000/FS)  # ms
        feats['hr']     = 60000/ rr.mean()
        feats['rmssd']  = np.sqrt(np.mean(np.diff(rr)**2))
        feats['sdnn']   = np.std(rr)
        # morphology: pulse width @ half amp
        half = (w.max()+w.min())/2
        # 간단히 첫 두 peak 구간 폭
        left = np.where(w[:peaks[0]]<half)[0]
        right= np.where(w[peaks[0]:]<half)[0]
        if left.size and right.size:
            feats['pulse_width'] = (right[0]+peaks[0]-left[-1])*(1000/FS)
        else:
            feats['pulse_width'] = np.nan
    else:
        feats.update({'hr':np.nan,'rmssd':np.nan,'sdnn':np.nan,'pulse_width':np.nan})
    return feats

# ——— 5) 전체 파이프라인 ———
def build_feature_df(subject):
    base = os.path.dirname(__file__)
    rec_dir = os.path.join(base, "recordings", subject)
    records=[]
    for fn in os.listdir(rec_dir):
        if not fn.endswith(".json"): continue
        ts,sig = load_ppg(os.path.join(rec_dir, fn))
        filt = bandpass(sig)
        # 윈도우마다 SQI 확인 → 기준 미만 삭제
        for w in window_split(filt):
            if compute_sqi(w) < SQI_THRESH:
                continue
            feats = extract_features(w)
            feats['label'] = fn.split('---')[-1].replace('.json','')
            records.append(feats)
    return pd.DataFrame(records).dropna()

# ——— 6) 간단 분류 테스트 ———
def quick_test(df):
    X = df.drop(columns='label').values
    y = df['label'].values
    clf = KNeighborsClassifier(3)
    scores = cross_val_score(clf, X, y, cv=5, scoring='accuracy')
    print(f"Windows: {len(df)}, k-NN acc: {scores.mean():.2f} ± {scores.std():.2f}")

# ——— 7) 실행부 ———
if __name__=="__main__":
    for subj in ["subject01","subject02","subject03"]:
        print(f"\n=== {subj} ===")
        df = build_feature_df(subj)
        quick_test(df)
        # CSV 저장
        out = f"outputs/features_{subj}.csv"
        os.makedirs("outputs", exist_ok=True)
        df.to_csv(out, index=False)
        print(f"Saved: {out}")
