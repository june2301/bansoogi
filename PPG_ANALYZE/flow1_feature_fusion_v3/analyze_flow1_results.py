# -*- coding: utf-8 -*-
"""
Fast Analysis for Flow1 Results (v3, optimized)
==============================================
- LOSO Confusion Matrix (RF)
- Class Distribution
- Permutation Importance
- Feature Importance

입력 파일: flow1_v3_data.npz
소요 시간: 약 3~5초
"""
import os
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')  # 비대화형 백엔드
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.metrics import confusion_matrix, classification_report, accuracy_score
from sklearn.ensemble import RandomForestClassifier
from sklearn.inspection import permutation_importance
from sklearn.model_selection import LeaveOneGroupOut
from collections import Counter

# ---------------- Load Data ----------------
d = np.load("flow1_v3_data.npz", allow_pickle=True)
X_raw = d["raw"]
X_feat = d["feat"]
y_raw = d["labels"]
groups = d["subjects"]

# Preprocess
le = LabelEncoder()
y = le.fit_transform(y_raw)
labels = le.classes_
scaler = StandardScaler()
X_feat = scaler.fit_transform(X_feat)

# Output dir
oos = "analysis_outputs"
os.makedirs(oos, exist_ok=True)

# ---------------- Class Distribution ----------------
c = Counter(y_raw)
plt.figure()
plt.bar(c.keys(), c.values(), color='skyblue')
plt.title("Class Distribution")
plt.xticks(rotation=45)
plt.tight_layout()
plt.savefig(os.path.join(oos, 'class_distribution.png'))
plt.close()

# ---------------- LOSO Confusion Matrix ----------------
logo = LeaveOneGroupOut()
y_true, y_pred = [], []
for tr, te in logo.split(X_feat, y, groups=groups):
    rf = RandomForestClassifier(n_estimators=100, random_state=42)
    rf.fit(X_feat[tr], y[tr])
    y_true.extend(y[te])
    y_pred.extend(rf.predict(X_feat[te]))
# Report
report = classification_report(y_true, y_pred, target_names=labels, output_dict=True)
print("LOSO Accuracy:", accuracy_score(y_true, y_pred))
# Save report CSV
pd.DataFrame(report).transpose().to_csv(os.path.join(oos, 'loso_classification_report.csv'))
# Confusion matrix plot
cm = confusion_matrix(y_true, y_pred)
plt.figure(figsize=(6,5))
sns.heatmap(cm, annot=True, fmt='d', xticklabels=labels, yticklabels=labels, cmap='Blues')
plt.title("LOSO Confusion Matrix")
plt.xlabel("Predicted")
plt.ylabel("True")
plt.tight_layout()
plt.savefig(os.path.join(oos, 'loso_confusion_matrix.png'))
plt.close()

# ---------------- Permutation Importance ----------------
# Use last trained rf model on full data
rf_full = RandomForestClassifier(n_estimators=100, random_state=42)
rf_full.fit(X_feat, y)
pi = permutation_importance(rf_full, X_feat, y, n_repeats=10, random_state=0, n_jobs=-1)
perm_imp = pi.importances_mean
feat_names = [f"feat_{i}" for i in range(X_feat.shape[1])]
plt.figure(figsize=(10,4))
plt.bar(range(len(perm_imp)), perm_imp, color='coral')
plt.xticks(range(len(perm_imp)), feat_names, rotation=90)
plt.title("Permutation Importances")
plt.tight_layout()
plt.savefig(os.path.join(oos, 'permutation_importances.png'))
plt.close()

# ---------------- Feature Importance (RF) ----------------
importances = rf_full.feature_importances_
plt.figure(figsize=(10,4))
plt.bar(range(len(importances)), importances, color='green')
plt.xticks(range(len(importances)), feat_names, rotation=90)
plt.title("Feature Importances (Random Forest)")
plt.tight_layout()
plt.savefig(os.path.join(oos, 'feature_importances.png'))
plt.close()

print(f"Analysis outputs saved to {oos}/")
