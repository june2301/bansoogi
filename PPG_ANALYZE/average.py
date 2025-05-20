import pandas as pd
from train_ppg_model import load_dataset, FEATS_10

# ① 전체 데이터를 로드
df = load_dataset()  # load_dataset() 은 train_ppg_model.py 에 정의된 함수

# ② subject·label 그룹별 feature 평균 계산
means = (
    df
    .groupby(['subject', 'label'])[FEATS_10]
    .mean()
    .reset_index()
)

print(means)
