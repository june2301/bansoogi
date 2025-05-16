"""윈도우 추출 + feature selection 저장"""
import os, json, math, numpy as np, pandas as pd
from scipy.signal import butter, filtfilt, welch, find_peaks
from sklearn.feature_selection import SelectKBest, mutual_info_classif
from sklearn.preprocessing import LabelEncoder, StandardScaler
from configs import FS, WIN_SEC, OVERLAP, BANDPASS, SQI_THRESH, K_BEST

# --- helpers ---
nyq = 0.5*FS
B_LO,B_H = BANDPASS
B, A = butter(4,[B_LO/nyq,B_H/nyq],btype='band')

def bandpass(x):
    return filtfilt(B, A, x)

def sqi(x):
    ac=x-x.mean(); rms=np.sqrt((ac**2).mean())
    b,a=butter(2,B_H/(0.5*FS),btype='high')
    noise=filtfilt(b,a,x)
    return 20*np.log10(rms/(np.sqrt((noise**2).mean())+1e-8))

def windows(sig):
    n=int(WIN_SEC*FS); step=int(n*(1-OVERLAP))
    for i in range(0,len(sig)-n+1,step):
        yield sig[i:i+n]

def feats(w):
    d={}
    d['dc']=w.mean(); d['ac']=w.ptp(); d['rms']=np.sqrt((w**2).mean())
    peaks,_=find_peaks(w,distance=int(0.2*FS))
    if len(peaks)>1:
        rr=np.diff(peaks)*(1000/FS)
        d['hr']=60000/rr.mean(); d['rmssd']=np.sqrt(np.mean(np.diff(rr)**2))
    else:
        d['hr']=0; d['rmssd']=0
    f,p=welch(w,fs=FS,nperseg=len(w)); tot=np.trapz(p,f)+1e-9
    mask_r=(f>=0.1)&(f<=0.4); mask_m=(f>=0.4)&(f<=2.0)
    d['psd_r']=np.trapz(p[mask_r],f[mask_r])/tot
    d['psd_m']=np.trapz(p[mask_m],f[mask_m])/tot
    return d

raws, feats_list, labels, subs = [],[],[],[]
for subj in sorted(os.listdir('recordings')):
    sd=os.path.join('recordings',subj)
    if not os.path.isdir(sd): continue
    for fn in sorted(os.listdir(sd)):
        if not fn.endswith('.json'): continue
        js=json.load(open(os.path.join(sd,fn),'r',encoding='utf-8'))
        sig=np.array(js['data']['ppg_continuous']['green'],float)
        bp=bandpass(sig)
        for w in windows(bp):
            if sqi(w)<SQI_THRESH: continue
            raws.append(w[:,None])
            feats_list.append(feats(w))
            labels.append(fn.split('---')[-1].replace('.json',''))
            subs.append(subj)
raw=np.stack(raws).astype('float32')
feat_df=pd.DataFrame(feats_list).fillna(0).astype('float32')
X_feat=feat_df.values
le=LabelEncoder(); y=le.fit_transform(labels)
# Select K best
skb=SelectKBest(mutual_info_classif,k=min(K_BEST,X_feat.shape[1]))
X_sel=skb.fit_transform(X_feat,y)
sel_indices=skb.get_support(indices=True)
# Save
np.savez_compressed('flow1_v5_data.npz', raw=raw, feat=X_sel, labels=y, subjects=np.array(subs), sel_idx=[9])
print('Saved flow1_v5_data.npz  |  samples=',len(y),'  | sel_feats=',sel_indices)