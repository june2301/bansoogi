"""데이터 전처리 및 윈도우 추출"""
import os, json
import numpy as np, pandas as pd
from scipy.signal import butter, filtfilt, find_peaks, welch
import math
from configs import FS, WIN_SEC, OVERLAP, BAND_LOW, BAND_HIGH, SQI_THRESH

# 밴드패스 필터
def bandpass(sig, low=BAND_LOW, high=BAND_HIGH, order=4):
    nyq = 0.5*FS
    b, a = butter(order, [low/nyq, high/nyq], btype='band')
    return filtfilt(b, a, sig)

# SQI 계산
def compute_sqi(sig):
    ac = sig - np.mean(sig)
    rms = math.sqrt(np.mean(ac**2))
    b, a = butter(2, BAND_HIGH/(0.5*FS), btype='high')
    noise = filtfilt(b, a, sig)
    return 20*math.log10(rms/(math.sqrt(np.mean(noise**2))+1e-8))

# 윈도우 분할
def sliding_windows(sig, win_sec=WIN_SEC, overlap=OVERLAP):
    n = int(win_sec * FS)
    step = int(n * (1-overlap))
    for i in range(0, len(sig)-n+1, step):
        yield sig[i:i+n]

# 2차 피처 추출
def extract_features(w):
    feats = {}
    # Morphology
    feats['dc']=np.mean(w); feats['ac']=np.ptp(w)
    # HRV
    peaks,_=find_peaks(w, distance=int(0.2*FS))
    if len(peaks)>1:
        rr=np.diff(peaks)*(1000/FS)
        feats['hr']=60000/rr.mean()
        feats['rmssd']=math.sqrt(np.mean(np.diff(rr)**2))
    else:
        feats['hr']=0; feats['rmssd']=0
    # Spectral
    f,p=welch(w,fs=FS,nperseg=len(w))
    band_r=(f>=0.1)&(f<=0.4); band_m=(f>=0.4)&(f<=2.0)
    tot=np.trapz(p,f)+1e-9
    feats['psd_r']=np.trapz(p[band_r],f[band_r])/tot
    feats['psd_m']=np.trapz(p[band_m],f[band_m])/tot
    return feats

# 주 진입 함수
if __name__=='__main__':
    raws, feats, labels, subs = [],[],[],[]
    for subj in sorted(os.listdir('recordings')):
        sd=os.path.join('recordings',subj)
        if not os.path.isdir(sd): continue
        for fn in sorted(os.listdir(sd)):
            if not fn.endswith('.json'): continue
            js=json.load(open(os.path.join(sd,fn),'r',encoding='utf-8'))
            sig=np.array(js['data']['ppg_continuous']['green'],dtype=float)
            bp_sig=bandpass(sig)
            for w in sliding_windows(bp_sig):
                if compute_sqi(w)<SQI_THRESH: continue
                raws.append(w)       # (WIN_SEC*FS,)
                feats.append(extract_features(w))
                labels.append(fn.split('---')[-1].replace('.json',''))
                subs.append(subj)
    X_raw = np.stack(raws).astype('float32')[:,:,None]
    df_feat=pd.DataFrame(feats).fillna(0).astype('float32')
    X_feat=df_feat.values
    np.savez_compressed('flow1_v4_data.npz', raw=X_raw, feat=X_feat,
                        labels=np.array(labels), subjects=np.array(subs))