import os
import json
import numpy as np
import pandas as pd
import logging
import matplotlib.pyplot as plt
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import kurtosis, skew, f_oneway
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix
# ★ RFECV 추가 import
from sklearn.feature_selection import RFECV

# ----------------------------------------
# 설정
# ----------------------------------------
FS = 25
WARMUP_SEC = 5
WINDOW_SEC = 10
OVERLAP = 0.0
BINS = 10
# FEATURE_NAMES = ['pwtf','kurtosis','skew']
FEATURE_NAMES = [
    'pnn50','rr_mean','hr_mean','rmssd','n_peaks',
    'crest_t','dwell_t','pwtf','kurtosis','skew'
]

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s [%(levelname)s] %(message)s')

LABEL_MAP = {
    'upright-sitting':'upright-sitting','앉기 자세 중립':'upright-sitting',
    'standing':'standing','서기 자세':'standing',
    'supine-lying':'supine-lying','눕혀진 누운 자세':'supine-lying'
}

# ----------------------------------------
# 1) 데이터 로딩
# ----------------------------------------
def load_ppg_files(base_dir):
    records=[]
    for subj in sorted(os.listdir(base_dir)):
        d=os.path.join(base_dir,subj)
        if not os.path.isdir(d): continue
        for fn in sorted(os.listdir(d)):
            if not fn.endswith('.json'): continue
            path=os.path.join(d,fn)
            data=json.load(open(path,'r',encoding='utf-8'))
            raw=data.get('label','')
            cands=[raw]
            if '/' in raw: cands += [p.strip() for p in raw.split('/')]
            mapped=None
            for rl in cands:
                if rl in LABEL_MAP:
                    mapped=LABEL_MAP[rl]; break
            if mapped is None:
                logging.warning(f"Skipping {path}: unmapped label {raw!r}")
                continue
            ts=np.array(data['data']['ppg_continuous']['ts'])
            sig=np.array(data['data']['ppg_continuous']['green'],dtype=float)
            records.append({'subject':subj,'label':mapped,'ts':ts,'sig':sig})
    logging.info(f"Loaded {len(records)} recordings")
    return records

# ----------------------------------------
# 2) 전처리 & 밴드패스
# ----------------------------------------
def butter_bandpass(sig,fs=FS,low=0.5,high=5.0,order=3):
    nyq=0.5*fs
    b,a=butter(order,[low/nyq,high/nyq],btype='band')
    return filtfilt(b,a,sig)

def preprocess_signal(ts,sig):
    start=ts[0]+WARMUP_SEC*1000
    s=sig[ts>=start]
    idx=np.arange(len(s))
    s=s - np.polyval(np.polyfit(idx,s,1), idx)
    return butter_bandpass(s, FS)

# ----------------------------------------
# 3) 윈도우 → 피처 추출 + Physio 필터
# ----------------------------------------
def extract_features_10(sig):
    feats={}
    x=sig
    peaks,_=find_peaks(x)
    troughs,_=find_peaks(-x)
    rr=np.diff(peaks)/FS if len(peaks)>1 else np.array([])
    diffs=np.diff(rr) if len(rr)>1 else np.array([])

    feats['n_peaks']=len(peaks)
    feats['rr_mean']= rr.mean() if rr.size else np.nan
    feats['hr_mean']= 60/feats['rr_mean'] if feats['rr_mean']>0 else np.nan
    feats['rmssd']= np.sqrt(np.mean(diffs**2)) if diffs.size else np.nan
    feats['pnn50']= np.sum(np.abs(diffs)>0.05)/diffs.size if diffs.size else np.nan

    cyc=[]
    for p in peaks:
        prev_t=max((t for t in troughs if t<p),default=None)
        next_t=min((t for t in troughs if t>p),default=None)
        if prev_t and next_t: cyc.append((prev_t,p,next_t))
    if cyc:
        ct=np.mean([(p-prev)/FS for prev,p,_ in cyc])
        dw=np.mean([(n-prev)/FS for prev,_,n in cyc])
        feats['crest_t']=ct; feats['dwell_t']=dw
        feats['pwtf']= ct/dw if dw>0 else np.nan
    else:
        feats.update({'crest_t':np.nan,'dwell_t':np.nan,'pwtf':np.nan})

    feats['kurtosis']= kurtosis(x) if x.size else np.nan
    feats['skew']    = skew(x)     if x.size else np.nan
    return feats

def windows_pipeline(records):
    rows=[]
    step=int(WINDOW_SEC*FS*(1-OVERLAP))
    win_len=int(WINDOW_SEC*FS)
    for rec in records:
        sig=preprocess_signal(rec['ts'],rec['sig'])
        for start in range(0,len(sig)-win_len+1,step):
            seg=sig[start:start+win_len]
            f=extract_features_10(seg)
            if not (40 < f['hr_mean'] < 120):      continue
            if not (0.5 < f['rr_mean'] < 1.5):     continue
            if not (f['n_peaks'] <= 30):          continue
            f['label']=rec['label']
            rows.append(f)
    return pd.DataFrame(rows)

# ----------------------------------------
# 4) Bin edge 생성
# ----------------------------------------
def compute_bins(bounds):
    bins_dict={}
    for f,(lo,hi) in bounds.items():
        bins_dict[f]=np.linspace(lo,hi,BINS+1)
    return bins_dict

# ----------------------------------------
# 5) 극단치 제거 (퍼센타일)
# ----------------------------------------
def percentile_filter(df, low=0.05, high=0.95):
    df2=df.copy()
    for f in FEATURE_NAMES:
        lo,hi=df[f].quantile([low,high])
        df2=df2[(df2[f]>=lo)&(df2[f]<=hi)]
    return df2

# ----------------------------------------
# 6) IQR 컷 bounds
# ----------------------------------------
def compute_global_bounds(df, iqr_multiplier=0.5):
    bounds={}
    for f in FEATURE_NAMES:
        s=df[f].dropna()
        q1,q3=np.percentile(s,[25,75])
        iqr=q3-q1
        bounds[f]=(q1-iqr_multiplier*iqr, q3+iqr_multiplier*iqr)
    return bounds

# ----------------------------------------
# 7) 평가
# ----------------------------------------
def evaluate(df):
    X=df[FEATURE_NAMES]
    encoder=LabelEncoder().fit(df['label'])
    y=encoder.transform(df['label'])
    clf=RandomForestClassifier(n_estimators=100,random_state=0)
    scores=cross_val_score(clf,X.fillna(0),y,cv=5)
    clf.fit(X.fillna(0),y)
    y_pred=clf.predict(X.fillna(0))
    report=classification_report(y,y_pred,target_names=encoder.classes_)
    cm=confusion_matrix(y,y_pred)
    pvals={f:(f_oneway(*(df[df['label']==lbl][f].dropna()
             for lbl in df['label'].unique()))[1]
             if all(len(df[df['label']==lbl][f].dropna())>=2
                    for lbl in df['label'].unique()) else np.nan)
           for f in FEATURE_NAMES}
    return scores,report,cm,pd.Series(pvals).sort_values()

# ----------------------------------------
# 8) 구간별 점유율
# ----------------------------------------
def compute_bin_occupancy(df, bin_edges):
    occupancy={}
    total=len(df)
    for f,edges in bin_edges.items():
        vals=df[f].clip(edges[0],edges[-1])
        counts,_=np.histogram(vals,bins=edges)
        occupancy[f]=counts/total
    return occupancy

# ----------------------------------------
# main
# ----------------------------------------
if __name__=='__main__':
    base_dir = r"C:\Users\SSAFY\Desktop\S12P31A302\PPG_ANALYZE\recordings"
    recs = load_ppg_files(base_dir)
    logging.info(f"Records found: {len(recs)}")

    # 윈도우→Physio 필터
    df_win = windows_pipeline(recs)
    logging.info(f"After physio filtering: {len(df_win)} windows")

    # 퍼센타일 컷
    df_clean = percentile_filter(df_win, low=0.00, high=1.00)
    logging.info(f"After 0-100% percentile filtering: {len(df_clean)} windows")

    # IQR 컷 bounds
    bounds = compute_global_bounds(df_clean, iqr_multiplier=0.5)
    logging.info("Calibration bounds (IQR‐0.5×):")
    for f,(lo,hi) in bounds.items():
        logging.info(f"  {f}: {lo:.4f}–{hi:.4f}")

    # 평가
    scores,report,cm,df_anova = evaluate(df_clean)
    print("Top-5 ANOVA features (p):\n", df_anova.head(5))
    print(f"5-fold CV acc: {scores.mean():.3f} ± {scores.std():.3f}")
    print("Classification report:\n", report)
    print("Confusion matrix:\n", cm)

    # ★ 9) RFECV 기반 자동 피처 선택
    X = df_clean[FEATURE_NAMES].fillna(0)
    y = LabelEncoder().fit_transform(df_clean['label'])
    rf = RandomForestClassifier(n_estimators=100, random_state=0)
    selector = RFECV(rf, cv=5, scoring='accuracy', min_features_to_select=3)
    selector.fit(X, y)
    selected = np.array(FEATURE_NAMES)[selector.support_]
    logging.info(f"RFECV selected {selector.n_features_} features: {selected.tolist()}")

    # 분포 시각화
    # for f in df_anova.index[:3]:
    #     plt.figure()
    #     for lbl in df_clean['label'].unique():
    #         plt.hist(df_clean[df_clean['label']==lbl][f], bins=BINS, alpha=0.5)
    #     plt.title(f"{f} distribution by class")
    #     plt.legend(df_clean['label'].unique())
    # plt.show()

    # 10) Bin edge 생성 및 점유율
    bin_edges = compute_bins(bounds)
    occupancy = compute_bin_occupancy(df_clean, bin_edges)
    logging.info("Bin occupancy (global) per feature:")
    for f, occ in occupancy.items():
        logging.info(f"  {f}: {np.round(occ,3)}")
