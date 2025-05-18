"""
ppg_pipeline.py  –  Stable version
---------------------------------
pip install lightgbm imbalanced-learn scipy pandas numpy scikit-learn
"""
from __future__ import annotations
import os, json, numpy as np, pandas as pd, warnings, gc
from pathlib import Path
from scipy.signal import butter, filtfilt, find_peaks, welch
from sklearn.model_selection import GroupKFold
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score
from lightgbm import LGBMClassifier
from imblearn.pipeline import Pipeline as ImbPipeline
from imblearn.over_sampling import SMOTE
from sklearn.ensemble import RandomForestClassifier
from sklearn.base import clone          # ★ NEW
warnings.filterwarnings("ignore", category=UserWarning)

# ════════════════════ 설정 ════════════════════
BASE_DIR   = Path(r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings")
WIN_MAIN, OVL_MAIN = 30, 0.25    # Stage‑2
WIN_FAST, OVL_FAST = 5,  0.50    # Stage‑1
FS = 25
LABELS = ['upright-sitting','supine-lying','standing']
FEATS  = ['pnn50','rr_mean','hr_mean','rmssd','n_peaks',
          'crest_t','dwell_t','pwtf','kurtosis','skew','lf_pow']

# ════════════════════ 신호 전처리 ════════════════════
def butter_band(sig, fs, low=0.5, high=5, order=3):
    nyq=0.5*fs; b,a= butter(order,[low/nyq,high/nyq],btype='band')
    return filtfilt(b,a,sig)

def preprocess(ts, sig, warm=5, fs=FS):
    s = sig[ts >= ts[0]+warm*1000]
    if len(s)<fs*3: return np.zeros(fs*3)
    trend=np.polyval(np.polyfit(np.arange(len(s)),s,1),np.arange(len(s)))
    return butter_band(s-trend, fs)

# ════════════════════ 피처 추출 ════════════════════
def extra_ppg(sig, fs=FS):
    pk,_=find_peaks(sig, distance=fs*0.4)
    tr,_=find_peaks(-sig, distance=fs*0.4)
    res=dict(crest_t=np.nan,dwell_t=np.nan,pwtf=np.nan)
    if not len(pk) or not len(tr): return res
    cyc=[]
    for p in pk:
        left=tr[tr<p]; right=tr[tr>p]
        if len(left)==0 or len(right)==0: continue
        f,n=left[-1],right[0]; cyc.append((f,p,n))
    if not cyc: return res
    ct=np.mean([(p-f)/fs for f,p,_ in cyc])
    dw=np.mean([(n-f)/fs for f,_,n in cyc])
    res.update(dict(crest_t=ct, dwell_t=dw, pwtf=ct/dw if dw else np.nan))
    return res

def hrv(sig, fs=FS):
    import pandas as pd
    v=pd.Series(sig.astype(float))
    pk,_=find_peaks(sig, distance=fs*0.4)
    rr=np.diff(pk)/fs
    d=dict(kurtosis=v.kurtosis(),skew=v.skew(),
           pnn50=np.nan,rr_mean=np.nan,hr_mean=np.nan,
           rmssd=np.nan,n_peaks=np.nan)
    if len(rr):
        d.update(dict(n_peaks=len(pk),rr_mean=rr.mean(),hr_mean=60/rr.mean()))
    if len(rr)>1:
        d.update(dict(rmssd=np.sqrt(np.mean(np.diff(rr)**2)),
                      pnn50=(np.abs(np.diff(rr))>0.05).mean()))
    f,P=welch(sig,fs=fs,nperseg=max(64,len(sig)//2))
    d['lf_pow']=P[(f>=0.7)&(f<=2)].sum()
    d.update(extra_ppg(sig,fs))
    return d

def slide(sig, win, ovl, fs=FS):
    N=int(win*fs); step=int(N*(1-ovl))
    for st in range(0,len(sig)-N+1,step):
        yield sig[st:st+N]

# ════════════════════ 데이터 적재 ════════════════════
def load_records():
    rec=[]
    for subj in sorted(p.name for p in BASE_DIR.iterdir() if p.is_dir()):
        for j in (BASE_DIR/subj).glob("*.json"):
            d=json.load(open(j,'r',encoding='utf-8'))
            rec.append(dict(subject=subj,
                            label=d['label'].split('/')[-1].strip(),
                            ts=np.array(d['data']['ppg_continuous']['ts']),
                            sig=np.array(d['data']['ppg_continuous']['green'])))
    return rec

def make_df(recs, win, ovl):
    rows=[]
    for r in recs:
        proc=preprocess(r['ts'], r['sig'])
        for seg in slide(proc, win, ovl):
            f=hrv(seg); f.update(subject=r['subject'],label=r['label'])
            rows.append(f)
    return pd.DataFrame(rows)

# ════════════════════ 모델 학습 함수 ════════════════════
def _logo_acc(model, X, y, groups):
    gkf=GroupKFold(n_splits=len(np.unique(groups)))
    pr,tru=[],[]
    for tr,te in gkf.split(X,y,groups):
        mdl=clone(model)          # ★ 복제 (Pipeline도 안전)
        mdl.fit(X.iloc[tr], y.iloc[tr])
        pr.extend(mdl.predict(X.iloc[te]))
        tru.extend(y.iloc[te])
    return accuracy_score(tru,pr)

def train_fast(df):
    X=df[FEATS[:7]].replace([np.inf,-np.inf],np.nan).fillna(0)
    y=df.label; g=df.subject
    rf=RandomForestClassifier(n_estimators=200,max_depth=8,
                              class_weight='balanced',random_state=0)
    acc=_logo_acc(rf,X,y,g); rf.fit(X,y)
    print(f"[Stage‑1] 5 s RF  acc={acc:.3f}")
    return rf

def train_main(df):
    X=df[FEATS].replace([np.inf,-np.inf],np.nan).fillna(0)
    y=df.label; g=df.subject
    pipe=ImbPipeline([
        ('smote', SMOTE(k_neighbors=2, random_state=42)),
        ('scaler', StandardScaler()),
        ('clf', LGBMClassifier(
            objective='multiclass',
            n_estimators=500,   learning_rate=0.1,
            num_leaves=31,      max_depth=-1,
            min_data_in_leaf=20,min_gain_to_split=0.0,
            feature_fraction=0.9, bagging_fraction=0.8,
            bagging_freq=1,     lambda_l2=1.0,
            force_col_wise=True, n_jobs=1,
            class_weight='balanced', random_state=42, verbose=-1))
    ])
    acc=_logo_acc(pipe,X,y,g)
    pipe.fit(X,y)             # group 인자 필요 없음
    print(f"[Stage‑2] 30 s LGBM acc={acc:.3f}")
    return pipe

# ════════════════════ 메인 루틴 ════════════════════
if __name__=="__main__":
    recs     = load_records()
    df_fast  = make_df(recs, WIN_FAST,  OVL_FAST)
    df_main  = make_df(recs, WIN_MAIN,  OVL_MAIN)

    model_fast = train_fast(df_fast)
    model_main = train_main(df_main)

    def predict_stream(seg_5s:np.ndarray):
        feat=hrv(seg_5s)
        p1=model_fast.predict_proba(pd.DataFrame([feat])[FEATS[:7]].fillna(0))[0]
        predict_stream.cache.append(feat)
        if len(predict_stream.cache)==WIN_MAIN//WIN_FAST:
            df_tmp=pd.DataFrame(predict_stream.cache)[FEATS].fillna(0)
            p2=model_main.predict_proba(df_tmp).mean(axis=0)
            predict_stream.cache.clear()
            return LABELS[int(np.argmax(p2))]
        return LABELS[int(np.argmax(p1))]
    predict_stream.cache=[]

    gc.collect()
