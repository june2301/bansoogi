import os
import json
import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, find_peaks

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

def extract_features(ts, sig, fs=25):
    """
    Extract time-domain & HRV features from a PPG segment.
    """
    # Basic stats
    feats = {
        'n_samples': len(sig),
        'mean': np.mean(sig),
        'std': np.std(sig),
        'min': np.min(sig),
        'max': np.max(sig),
        'diff_std': np.std(np.diff(sig)),
        'skew': pd.Series(sig).skew(),
        'kurtosis': pd.Series(sig).kurtosis(),
    }

    # Peak detection for HRV
    peaks, _ = find_peaks(sig, distance=fs*0.4)  # at least 0.4s between peaks
    rr_intervals = np.diff(ts[peaks]) / 1000.0  # in seconds
    if len(rr_intervals) > 1:
        feats.update({
            'n_peaks': len(peaks),
            'hr_mean': 60.0 / np.mean(rr_intervals),
            'rr_mean': np.mean(rr_intervals),
            'rr_std': np.std(rr_intervals),
            'rmssd': np.sqrt(np.mean(np.diff(rr_intervals)**2)),
            'pnn50': np.sum(np.abs(np.diff(rr_intervals)) > 0.05) / len(rr_intervals),
        })
    else:
        feats.update({
            'n_peaks': len(peaks),
            'hr_mean': np.nan,
            'rr_mean': np.nan,
            'rr_std': np.nan,
            'rmssd': np.nan,
            'pnn50': np.nan,
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
        feats = extract_features(ts_clean, sig_clean, fs=fs)
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
