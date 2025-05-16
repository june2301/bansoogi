# -*- coding: utf-8 -*-
"""윈도우·2차 피처 추출 → flow1_v2_data.npz"""
import os, json, math
import numpy as np, pandas as pd
from scipy.signal import butter, filtfilt, welch, find_peaks
from scipy.stats import skew, kurtosis
from configs import *

# DSP helpers -------------------------------------------------
def bp(sig, band, order=4):
    nyq = FS*0.5; b,a = butter(order, [band[0]/nyq, band[1]/nyq], btype="band")
    return filtfilt(b, a, sig)

def sqi(sig):
    ac = sig - sig.mean(); rms = math.sqrt(np.mean(ac**2))
    b,a = butter(2, BAND_RAW[1]/(0.5*FS), btype="high")
    noise = filtfilt(b,a,sig); rms_n = math.sqrt(np.mean(noise**2))
    return 20*math.log10(rms/(rms_n+1e-8))

def windows(sig, win_sec):
    n = int(win_sec*FS); step = int(n*(1-OVERLAP_RATE))
    for i in range(0, len(sig)-n+1, step):
        yield sig[i:i+n]

# Feature extract --------------------------------------------
def morph_feats(w):
    diff1 = np.gradient(w); diff2 = np.gradient(diff1)
    peaks,_ = find_peaks(w, distance=int(0.2*FS))
    rr_ms = np.diff(peaks)*(1000/FS) if len(peaks)>1 else [0]
    notch_ratio = (w.min()-w.mean())/(w.max()-w.min()+1e-6)
    return dict(
        dc=w.mean(), ac_amp=w.ptp(), rms=np.sqrt(np.mean(w**2)),
        skew=skew(w), kurtosis=kurtosis(w), zcr=np.sum(w[:-1]*w[1:]<0)/len(w),
        d1_mean=diff1.mean(), d2_mean=diff2.mean(),
        hr=60000/np.mean(rr_ms) if rr_ms[0] else 0,
        rmssd=np.sqrt(np.mean(np.diff(rr_ms)**2)) if len(rr_ms)>2 else 0,
        notch=notch_ratio
    )

def spec_feats(w):
    f, p = welch(w, fs=FS, nperseg=len(w))
    tot = np.trapz(p,f)+1e-9
    mask_r = (f>=0.1)&(f<=0.4); mask_m = (f>=0.4)&(f<=2.0)
    return dict(
        psd_r=np.trapz(p[mask_r],f[mask_r])/tot,
        psd_m=np.trapz(p[mask_m],f[mask_m])/tot,
        band_ratio=(np.trapz(p[mask_r],f[mask_r])+1e-9)/(np.trapz(p[mask_m],f[mask_m])+1e-9),
        spec_ent=-np.sum((p/tot)*np.log(p/tot+1e-12))
    )

# Main --------------------------------------------------------
def build(root="recordings", out="flow1_v2_data.npz"):
    raws, feats, labels, subj = [],[],[],[]
    for s in sorted(os.listdir(root)):
        d=os.path.join(root,s); 
        if not os.path.isdir(d): continue
        for fn in sorted(os.listdir(d)):
            if not fn.endswith(".json"): continue
            data=json.load(open(os.path.join(d,fn),"r",encoding="utf-8"))
            sig=np.array(data["data"]["ppg_continuous"]["green"],dtype=float)
            label=fn.split("---")[-1].replace(".json","")
            # feature windows drive loop
            feat_sig=bp(sig,BAND_FEAT); raw_sig=bp(sig,BAND_RAW)
            for w_f in windows(feat_sig, WIN_FEAT_SEC):
                if sqi(w_f)<SQI_THRESH: continue
                feats.append({**morph_feats(w_f), **spec_feats(w_f)})
                cut=raw_sig[:int(WIN_RAW_SEC*FS)]
                raw_pad=np.pad(cut,(0,max(0,int(WIN_RAW_SEC*FS)-len(cut))),mode='edge')
                raws.append(raw_pad)
                labels.append(label); subj.append(s)
    X_raw=np.stack(raws).astype("float32").reshape(-1,int(WIN_RAW_SEC*FS),1)
    X_feat=pd.DataFrame(feats).fillna(0).astype("float32").values
    np.savez_compressed(out, raw=X_raw, feat=X_feat,
                        labels=np.array(labels), subjects=np.array(subj))
    print(f"Saved {out} | N={len(labels)}")

if __name__=="__main__":
    build()
