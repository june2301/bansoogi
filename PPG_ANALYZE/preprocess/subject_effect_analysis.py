"""
subject_effect_analysis.py  —  라벨 고정 시 피험자 영향 탐색
pip install scipy pandas numpy seaborn matplotlib statsmodels pingouin
"""
import json, numpy as np, pandas as pd, seaborn as sns, matplotlib.pyplot as plt
from pathlib import Path
from scipy.signal import butter, filtfilt, find_peaks, welch
import statsmodels.formula.api as smf, statsmodels.api as sm
from pingouin import intraclass_corr
plt.rcParams['figure.dpi'] = 110
sns.set(style="whitegrid")

# ─── 사용자 설정 ────────────────────────────────────────────────
BASE_DIR = Path(r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings")
WIN_S, OVL = 30, 0.25      # 윈도우 길이·오버랩
FS = 25
LABELS = ['upright-sitting', 'supine-lying', 'standing']
FEATS = ['pnn50','rr_mean','hr_mean','rmssd','n_peaks',
         'crest_t','dwell_t','pwtf','kurtosis','skew','lf_pow']

# ─── 전처리 & 피처 함수 (ppg_pipeline 의 경량 버전) ─────────────
def butter_band(sig, fs, low=0.5, high=5, order=3):
    nyq=0.5*fs; b,a=butter(order,[low/nyq,high/nyq],'band')
    return filtfilt(b,a,sig)

def preprocess(ts, sig, warm=5, fs=FS):
    sig=sig[ts >= ts[0]+warm*1000]
    if len(sig) < fs*3: return np.zeros(fs*3)
    trend=np.polyval(np.polyfit(np.arange(len(sig)),sig,1),np.arange(len(sig)))
    return butter_band(sig-trend,fs)

def extra_ppg_features(sig, fs):
    pk,_=find_peaks(sig,distance=fs*0.4)
    tr,_=find_peaks(-sig,distance=fs*0.4)
    res=dict(crest_t=np.nan,dwell_t=np.nan,pwtf=np.nan)
    if len(pk)==0 or len(tr)==0: return res
    cyc=[]
    for p in pk:
        lf=tr[tr<p]; rt=tr[tr>p]
        if len(lf)==0 or len(rt)==0: continue
        a,b,c=lf[-1],p,rt[0]; cyc.append((a,b,c))
    if not cyc: return res
    ct=np.mean([(b-a)/fs for a,b,_ in cyc])
    dw=np.mean([(c-a)/fs for a,_,c in cyc])
    res.update(dict(crest_t=ct,dwell_t=dw,pwtf=ct/dw if dw else np.nan))
    return res

def hrv_features(sig, fs=FS):
    s=pd.Series(sig.astype(float))
    pk,_=find_peaks(sig,distance=fs*0.4); rr=np.diff(pk)/fs
    d=dict(kurtosis=s.kurtosis(),skew=s.skew(),
           pnn50=np.nan,rr_mean=np.nan,hr_mean=np.nan,
           rmssd=np.nan,n_peaks=np.nan)
    if len(rr):
        d.update(n_peaks=len(pk),rr_mean=rr.mean(),hr_mean=60/rr.mean())
    if len(rr)>1:
        d.update(rmssd=np.sqrt(np.mean(np.diff(rr)**2)),
                 pnn50=(np.abs(np.diff(rr))>0.05).mean())
    f,P=welch(sig,fs=fs,nperseg=max(64,len(sig)//2))
    d['lf_pow']=P[(f>=0.7)&(f<=2)].sum()
    d.update(extra_ppg_features(sig,fs))
    return d

def slide(sig, win, ovl, fs=FS):
    n=int(win*fs); step=int(n*(1-ovl))
    for st in range(0,len(sig)-n+1,step):
        yield sig[st:st+n]

# ─── 데이터 → DF ───────────────────────────────────────────────
def load_df():
    rows=[]
    for subj in sorted(p.name for p in BASE_DIR.iterdir() if p.is_dir()):
        for jf in (BASE_DIR/subj).glob("*.json"):
            d=json.load(open(jf,'r',encoding='utf-8'))
            lab=d['label'].split('/')[-1].strip()
            ts=np.array(d['data']['ppg_continuous']['ts'])
            sig=np.array(d['data']['ppg_continuous']['green'])
            sig=preprocess(ts,sig)
            for seg in slide(sig,WIN_S,OVL):
                f=hrv_features(seg)
                f.update(subject=subj,label=lab)
                rows.append(f)
    df=pd.DataFrame(rows)
    print("DataFrame shape:", df.shape)
    return df

# ─── 분석 함수 ──────────────────────────────────────────────────
def analyse_feature(df, feat):
    print(f"\n──────── {feat} ────────")

    # 1) box+swarm (피험자별 색)
    plt.figure(figsize=(6,3))
    sns.boxplot(x='label',y=feat,data=df,showfliers=False)
    sns.swarmplot(x='label',y=feat,data=df,hue='subject',
                  dodge=True,size=3,linewidth=0.3)
    plt.title(f'{feat} per subject'); plt.tight_layout(); plt.show()

    # 2) 두‑요인 ANOVA
    aov=smf.ols(f'{feat} ~ C(label) + C(subject)',data=df).fit()
    tbl=sm.stats.anova_lm(aov,typ=2)
    print("Two‑way ANOVA\n", tbl[['F','PR(>F)']])

    # 3) 혼합효과 (subject random)
    mix=smf.mixedlm(f'{feat} ~ C(label)',df,groups=df['subject']).fit()
    print("\nMixed‑Effect (subject random):")
    print(mix.summary().tables[1])

    # 4) ICC(2,1)  – 균형 테이블 확보
    piv=(df[['subject','label',feat]]
         .dropna()
         .groupby(['subject','label'])[feat].mean()
         .unstack())            # rows=subject
    bal=piv.dropna()
    if bal.shape[0] < 3:
        print("\nICC: not enough balanced data (need ≥3 subjects).")
    else:
        ic_in=(bal.reset_index()
               .melt(id_vars='subject',var_name='label',value_name=feat))
        icc=intraclass_corr(ic_in, targets='subject', raters='label',
                            ratings=feat, nan_policy='omit')
        # 열 이름이 Type / type 일 수 있으므로 lower() 비교
        col = next(c for c in icc.columns if c.lower()=="type")
        mask = icc[col].str.upper()=="ICC2"
        if mask.any():
            row=icc.loc[mask].iloc[0]
            icc_val=row['ICC']; lo,hi=row['CI95%']
            print(f"\nICC2 (consistency): {icc_val:.3f}  "
                  f"[95 % CI {lo:.3f} – {hi:.3f}]")
        else:
            print("\nICC: output did not contain ICC2.")

    # 5) subject‑wise z‑score violin
    df_z=df.copy()
    for s,g in df.groupby('subject'):
        df_z.loc[g.index,feat]=(g[feat]-g[feat].mean())/g[feat].std()
    plt.figure(figsize=(5,3))
    sns.violinplot(x='label',y=feat,data=df_z,inner='quartile')
    plt.title(f'{feat} after subject z‑score'); plt.tight_layout(); plt.show()

# ─── 메인 실행 ──────────────────────────────────────────────────
if __name__=="__main__":
    import pandas as pd
    df=load_df()

    inspect_feats=['hr_mean','pnn50','kurtosis']  # 필요 피처 목록
    for f in inspect_feats:
        if f in df.columns and df[f].notna().sum():
            analyse_feature(df,f)
        else:
            print(f"{f}: not available or all NaN")
