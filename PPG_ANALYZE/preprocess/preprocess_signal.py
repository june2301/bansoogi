import os
import json
import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks
from scipy.stats import skew, kurtosis


def load_ppg_files(base_dir):
    """
    Load all JSON files from the recordings directory.
    Returns a list of dicts with keys: subject, label, ts, green, red, ir
    """
    records = []
    for subject in sorted(os.listdir(base_dir)):
        subj_dir = os.path.join(base_dir, subject)
        if not os.path.isdir(subj_dir):
            continue
        for fname in sorted(os.listdir(subj_dir)):
            if not fname.endswith('.json'):
                continue
            path = os.path.join(subj_dir, fname)
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            rec = {
                'subject': subject,
                'label': data['label'],
                'start_ts': data['start_ts'],
                'duration_s': data['duration_s'],
                'ts': np.array(data['data']['ppg_continuous']['ts']),
                'green': np.array(data['data']['ppg_continuous']['green']),
                'red': np.array(data['data']['ppg_continuous']['red']),
                'ir': np.array(data['data']['ppg_continuous']['ir']),
            }
            records.append(rec)
    return records

def crest_dwell_pwtf(sig, peaks, troughs, fs=25):
    """
    Extract crest time, dwell time, and pulse width time fraction (pwtf) from signal.
    """
    triples = []
    for p in peaks:
        left = troughs[troughs < p]
        right = troughs[troughs > p]
        if left.size and right.size:
            triples.append((left.max(), p, right.min()))

    if triples:
        crest_t = np.mean([(p - f) / fs for f, p, _ in triples])
        dwell_t = np.mean([(n - f) / fs for f, _, n in triples])
        pwtf = crest_t / dwell_t if dwell_t else 0
    else:
        crest_t, dwell_t, pwtf = np.nan, np.nan, np.nan

    return crest_t, dwell_t, pwtf

def butter_bandpass_filter(signal, fs, lowcut=0.5, highcut=5.0, order=3):
    nyq = 0.5 * fs
    low = lowcut / nyq
    high = highcut / nyq
    b, a = butter(order, [low, high], btype='band')
    return filtfilt(b, a, signal)

def preprocess_signal(ts, sig, warmup_sec=5, fs=25, apply_filter=True):
    """
    Remove initial warm-up period and optionally apply bandpass filter.
    """
    # Remove warm-up samples
    start_time = ts[0] + warmup_sec * 1000  # ms
    mask = ts >= start_time
    ts_clean = ts[mask]
    sig_clean = sig[mask]
    
    # Detrend (remove linear trend)
    sig_clean = sig_clean - np.polyval(np.polyfit(np.arange(len(sig_clean)), sig_clean, 1), np.arange(len(sig_clean)))
    
    # Optional filtering
    if apply_filter:
        sig_clean = butter_bandpass_filter(sig_clean, fs)
    
    return ts_clean, sig_clean

def full_extract_features(ts, sig, fs=25):
    feats = {
        'mean': np.mean(sig),
        'std': np.std(sig),
        'rms': np.sqrt(np.mean(sig**2)),
        'max': np.max(sig),
        'min': np.min(sig),
        'zc': ((sig[:-1] * sig[1:]) < 0).sum(),
        'ssc': np.sum(np.diff(np.sign(np.diff(sig))) != 0),
        'wl': np.sum(np.abs(np.diff(sig))),
        'kurt': kurtosis(sig),
        'skew': skew(sig)
    }

    peaks, _ = find_peaks(sig, distance=fs*0.4)
    troughs, _ = find_peaks(-sig, distance=fs*0.4)
    rr = np.diff(ts[peaks]) / 1000.0

    if len(rr) > 1:
        feats.update({
            'pnn50': np.sum(np.abs(np.diff(rr)) > 0.05) / len(rr),
            'rr_mean': np.mean(rr),
            'hr_mean': 60. / np.mean(rr),
            'rmssd': np.sqrt(np.mean(np.diff(rr)**2)),
            'n_peaks': len(peaks),
        })
    else:
        feats.update({
            'pnn50': np.nan,
            'rr_mean': np.nan,
            'hr_mean': np.nan,
            'rmssd': np.nan,
            'n_peaks': len(peaks),
        })

    crest_t, dwell_t, pwtf = crest_dwell_pwtf(sig, peaks, troughs, fs)
    feats.update({
        'crest_t': crest_t,
        'dwell_t': dwell_t,
        'pwtf': pwtf
    })

    return feats

def build_feature_dataframe(records, warmup_sec=5, fs=25, apply_filter=True):
    """
    Process all records, extract features per-record, return pandas DataFrame.
    """
    all_feats = []
    for rec in records:
        ts_clean, sig_clean = preprocess_signal(rec['ts'], rec['green'],
                                                warmup_sec=warmup_sec, fs=fs,
                                                apply_filter=apply_filter)
        feats = full_extract_features(ts_clean, sig_clean, fs=fs)
        feats.update({
            'subject': rec['subject'],
            'label': rec['label'],
            'warmup_sec': warmup_sec,
            'filtered': apply_filter
        })
        all_feats.append(feats)
    
    df = pd.DataFrame(all_feats)
    return df

if __name__ == "__main__":
    BASE_DIR = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "recordings")
    )
    records = load_ppg_files(BASE_DIR)
    
    # Generate features per-subject and label
    df_features = build_feature_dataframe(records, warmup_sec=5, apply_filter=True)
    
    # Save or inspect
    df_features.to_csv("ppg_features_per_record.csv", index=False)
    print("Feature extraction complete. Saved to ppg_features_per_record.csv.")
