import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy.stats import f_oneway
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import LabelEncoder

# 1) 데이터 불러오기
df = pd.read_csv("ppg_features_per_record.csv")

# 2) 분석할 feature만 뽑기
ignore_cols = ['subject', 'label', 'warmup_sec', 'filtered']
features = [c for c in df.columns if c not in ignore_cols]

# 3) 각 label별 분포 시각화 (boxplot)
labels = df['label'].unique()
for feat in features:
    data_to_plot = [df[df['label']==lbl][feat].dropna() for lbl in labels]
    plt.figure()
    plt.boxplot(data_to_plot, labels=labels)
    plt.title(f"{feat} by posture")
    plt.ylabel(feat)
    plt.xlabel("posture")
    plt.tight_layout()
    plt.show()

# 4) 통계적 유의미성 검사: ANOVA
anova_p = {}
for feat in features:
    groups = [df[df['label']==lbl][feat].dropna() for lbl in labels]
    stat, p = f_oneway(*groups)
    anova_p[feat] = p

df_anova = pd.Series(anova_p, name="p_value").sort_values()
print("=== ANOVA p-values (ascending) ===")
print(df_anova)

# 5) 분류기를 이용한 feature importance
#   - 레이블 인코딩
le = LabelEncoder()
y = le.fit_transform(df['label'])

#   - 결측치는 0으로 채우고, X/Y 준비
X = df[features].fillna(0)

#   - RandomForest 학습
clf = RandomForestClassifier(n_estimators=200, random_state=42)
clf.fit(X, y)

#   - 중요도 추출
imp = pd.Series(clf.feature_importances_, index=features).sort_values(ascending=False)
print("\n=== Feature importances ===")
print(imp)

# 6) 교차검증 정확도 확인
cv_scores = cross_val_score(clf, X, y, cv=5)
print(f"\n5‑fold CV accuracy: {cv_scores.mean():.3f} ± {cv_scores.std():.3f}")
