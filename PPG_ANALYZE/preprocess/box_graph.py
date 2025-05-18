import pandas as pd
import matplotlib.pyplot as plt

# 한글 폰트 설정 (Windows 환경: Malgun Gothic)
plt.rcParams['font.family'] = 'Malgun Gothic'
plt.rcParams['axes.unicode_minus'] = False

# CSV 불러오기
df = pd.read_csv("ppg_features_per_record.csv")

# 시각화할 HRV 피처 리스트
features = ['pnn50', 'rr_mean', 'hr_mean', 'n_peaks', 'rmssd']
# 실제 데이터 라벨(영어) 순서
orig_labels = ['upright-sitting', 'supine-lying', 'standing']
# 한글 레이블 매핑
label_names = {'upright-sitting': '앉음', 'supine-lying': '눕음', 'standing': '섬'}

# 박스플롯 그리기
for feat in features:
    data = [df[df['label'] == lbl][feat].dropna() for lbl in orig_labels]
    fig, ax = plt.subplots(figsize=(6, 4))
    # tick_labels 인자로 카테고리 텍스트 지정
    ax.boxplot(data, tick_labels=[label_names[l] for l in orig_labels], showfliers=True)
    ax.set_title(f"{feat} 분포 by 자세")
    ax.set_xlabel("자세")
    ax.set_ylabel(feat)
    ax.grid(axis='y', linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.show()
