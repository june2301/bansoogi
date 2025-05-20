import os
import json
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import butter, filtfilt

# ——— 설정 ———
FS = 25.0            # 샘플링 주파수 (Hz)
LOWCUT = 0.4         # 밴드패스 하한 (Hz)
HIGHCUT = 8.0        # 밴드패스 상한 (Hz)
ORDER = 4            # 필터 차수

# ——— 1) JSON 로드 ———
def load_ppg(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        d = json.load(f)
    data = d["data"]["ppg_continuous"]
    ts = np.array(data["ts"])             # 상대 타임스탬프 (ms)
    sig = np.array(data["green"], dtype=float)
    return ts, sig

# ——— 2) 밴드패스 필터 ———
def bandpass_filter(sig, fs=FS, low=LOWCUT, high=HIGHCUT, order=ORDER):
    nyq = 0.5 * fs
    b, a = butter(order, [low/nyq, high/nyq], btype='band')
    return filtfilt(b, a, sig)

# ——— 3) SQI 계산 ———
def compute_sqi(sig, fs=FS):
    ac = sig - np.mean(sig)
    sig_rms = np.sqrt(np.mean(ac**2))
    # 노이즈는 고주파 성분(>HIGHCUT Hz)으로 간주
    b, a = butter(2, HIGHCUT/(0.5*fs), btype='high')
    noise = filtfilt(b, a, sig)
    noise_rms = np.sqrt(np.mean(noise**2))
    if noise_rms == 0:
        return np.inf
    return 20 * np.log10(sig_rms / noise_rms)

# ——— 4) 결과 플롯 저장 ———
def plot_signals(ts, raw, filt, out_dir, base_name):
    t = (ts - ts[0]) / 1000.0  # 초 단위
    plt.figure(figsize=(8, 3))
    plt.plot(t, raw, label='Raw')
    plt.plot(t, filt, label='Filtered')
    plt.xlabel('Time (s)')
    plt.ylabel('Amplitude')
    plt.legend(loc='upper right')
    plt.tight_layout()
    os.makedirs(out_dir, exist_ok=True)
    plt.savefig(os.path.join(out_dir, f"{base_name}_filter.png"))
    plt.close()

# ——— 5) 메인 처리 루프 ———
def main():
    base_dir = os.path.dirname(__file__)
    rec_dir = os.path.join(base_dir, "recordings", "subject01")
    out_dir = os.path.join(base_dir, "outputs")

    if not os.path.isdir(rec_dir):
        print("Error: recordings/subject01 폴더를 찾을 수 없습니다.")
        return

    files = sorted(f for f in os.listdir(rec_dir) if f.endswith(".json"))
    if not files:
        print("No JSON files found in", rec_dir)
        return

    for fn in files:
        path = os.path.join(rec_dir, fn)
        print(f"\n▶ 처리 중: {fn}")
        ts, raw = load_ppg(path)
        filt = bandpass_filter(raw)
        sqi = compute_sqi(filt)
        print(f"  • SQI: {sqi:.2f} dB")
        plot_signals(ts, raw, filt, out_dir, fn.replace('.json',''))

    print("\n모든 파일 처리 완료. outputs/ 에 결과 플롯이 저장되었습니다.")

if __name__ == "__main__":
    main()
