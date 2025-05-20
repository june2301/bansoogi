"""
Session‑level 라벨×피험자×세션 효과 분석
────────────────────────────────────
pip install pandas numpy scipy statsmodels pingouin seaborn matplotlib
"""
from __future__ import annotations
import os, json, warnings, datetime as dt
import numpy as np, pandas as pd
import seaborn as sns, matplotlib.pyplot as plt
from pathlib import Path
from scipy.signal import butter, filtfilt, find_peaks
import statsmodels.api as sm, statsmodels.formula.api as smf
from pingouin import intraclass_corr

warnings.filterwarnings("ignore", category=UserWarning)
warnings.filterwarnings("ignore", category=FutureWarning)

# ────────── 설정 ──────────
BASE_DIR   = Path(r"C:\Users\lasni\Desktop\S12P31A302\PPG_ANALYZE\recordings")
OUT_DIR    = Path("results_session"); OUT_DIR.mkdir(exist_ok=True)
FS         = 25
FEATS      = ['hr_mean','pnn50','kurtosis','rr_mean','rmssd']
LABEL_MAP  = {'upright-sitting':'sit',
              '바른 자세로 앉기 / upright-sitting':'sit',
              'supine-lying':'sup',
              'standing':'std'}
MIN_VALID  = 3          # feature 5개 중 최소 3개만 채워져도 사용
PEAK_DIST  = 0.35       # sec (조금 낮춤)

# ────────── helpers ──────────
def _band(sig, fs=FS, low=0.5, high=5.0, order=3):
    nyq = 0.5*fs
    b,a = butter(order,[low/nyq,high/nyq],'band')
    if len(sig) < 3*max(len(a), len(b)):      # filtfilt padLen
        return sig * 0                        # too short → zero
    return filtfilt(b,a,sig)

def _features(ts, sig):
    if len(sig) < FS*2:      # 2초 미만 → 전부 NaN
        return {k:np.nan for k in FEATS}
    sig = _band(sig - np.polyval(np.polyfit(
                np.arange(len(sig)), sig, 1), np.arange(len(sig))))
    # peaks
    dist = int(PEAK_DIST*FS)
    pk,_ = find_peaks(sig, distance=dist, prominence=np.std(sig)*0.1)
    rr = np.diff(pk)/FS
    feats = {
        'kurtosis': pd.Series(sig).kurtosis(),
        'rr_mean' : rr.mean() if len(rr) else np.nan,
        'hr_mean' : 60/rr.mean() if len(rr) else np.nan,
        'rmssd'   : np.sqrt(np.mean(np.diff(rr)**2)) if len(rr)>1 else np.nan,
        'pnn50'   : (np.abs(np.diff(rr))>0.05).mean() if len(rr)>1 else np.nan,
    }
    return feats

def _load_df() -> pd.DataFrame:
    rows=[]
    for subj_dir in BASE_DIR.iterdir():
        if not subj_dir.is_dir(): continue
        subj = subj_dir.name
        for jf in subj_dir.glob("*.json"):
            d = json.load(open(jf, encoding="utf-8"))
            raw_label = d['label'].replace('\\/', '/')
            lab = LABEL_MAP.get(raw_label.split('/')[-1].strip(), raw_label)
            ts  = np.asarray(d['data']['ppg_continuous']['ts'])
            sig = np.asarray(d['data']['ppg_continuous']['green'])
            # session id = YYYYMMDD_HHMMSS
            date = dt.datetime.fromtimestamp(ts[0]/1000,
                                             dt.timezone.utc).strftime("%Y%m%d_%H%M%S")
            sess = f"{subj}_{date}"
            feats = d.get('features', {})
            if not feats:        # 직접 계산
                feats = _features(ts, sig)
            rows.append({'subject':subj,'session':sess,'label':lab, **feats})
    df = pd.DataFrame(rows)
    # 최소 valid feature 수 만족
    mask_valid = df[FEATS].notna().sum(axis=1) >= MIN_VALID
    removed = (~mask_valid).sum()
    if removed:
        print(f"· {removed} rows removed (<{MIN_VALID} valid features)")
    return df[mask_valid]

df = _load_df()
print("DataFrame shape:", df.shape)

# ────────── 분석 함수 ──────────
def analyse_feature(feat:str):
    print(f"\n──── {feat} ────")
    sub = df[['subject','session','label',feat]].dropna()
    if sub.empty:
        print("no data"); return

    # 1) Two‑way ANOVA
    aov = sm.stats.anova_lm(
            smf.ols(f"{feat} ~ C(label)+C(subject)", data=sub).fit(), typ=2)
    print(aov[['F','PR(>F)']])
    aov.to_csv(OUT_DIR/f"{feat}_anova.csv")

    # 2) Mixed‑effect (subject random, session nested)
    try:
        mm  = smf.mixedlm(f"{feat} ~ C(label)", sub,
                          groups='subject',
                          vc_formula={'session':'0+C(session)'})
        mres= mm.fit(reml=False)
        var_sub, var_sess = float(mres.cov_re.iloc[0,0]), mres.vcomp['session Var']
        print(f"σ²_subject={var_sub:.3f}  σ²_session={var_sess:.3f}")
        mres.summary().tables[1].to_csv(OUT_DIR/f"{feat}_mixedlm.csv")
    except Exception as e:
        print("mixedlm 실패:", e)

    # 3) ICC(3,k)
    try:
        ic = intraclass_corr(sub, targets="subject",
                             raters="session", ratings=feat,
                             nan_policy='omit')
        ic3 = ic.query("Type=='ICC3_k'").iloc[0]
        print(f"ICC3_k={ic3['ICC']:.3f}  CI=({ic3['CI95%'][0]:.3f}, {ic3['CI95%'][1]:.3f})")
        ic.to_csv(OUT_DIR/f"{feat}_icc.csv", index=False)
    except Exception as e:
        print("ICC 계산 실패:", e)

    # 4) violin plot (z‑score by subject)
    z = sub.copy()
    z[feat] = z.groupby('subject')[feat].transform(
                lambda x:(x-x.mean())/x.std(ddof=0))
    plt.figure(figsize=(6,3))
    sns.violinplot(data=z, x='label', y=feat, inner='quartile',
                   palette="Set2", cut=0)
    plt.title(f"{feat} (subject‑z)"); plt.tight_layout()
    plt.savefig(OUT_DIR/f"{feat}_violin.png", dpi=200); plt.close()

# ────────── 실행 ──────────
for f in FEATS:
    analyse_feature(f)

print(f"\n✓ 결과가 '{OUT_DIR}/' 폴더에 저장되었습니다.")
