from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score

# df 는 load_and_window_dataset() 반환 DataFrame
X = df[FEATS_10].fillna(0).to_numpy()
y = df['label'].map({l:i for i,l in enumerate(LABELS)}).to_numpy()

rf = RandomForestClassifier(n_estimators=100, random_state=42)
scores = cross_val_score(rf, X, y, cv=LeaveOneGroupOut().split(X, y, df['subject']), scoring='accuracy')
print(f"RandomForest LOSO‑CV: {scores.mean():.3f} ± {scores.std():.3f}")
