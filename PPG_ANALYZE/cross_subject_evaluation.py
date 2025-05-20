import os
import pandas as pd
import numpy as np
from sklearn.neighbors import KNeighborsClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import LeaveOneGroupOut, GridSearchCV, cross_val_score

# ——— 설정 ———
INPUT_DIR = os.path.join(os.path.dirname(__file__), 'outputs')  # features CSV 위치
RESULTS_DIR = os.path.join(os.path.dirname(__file__), 'results')
os.makedirs(RESULTS_DIR, exist_ok=True)

# ——— 1) 데이터 로드 ———
def load_all_features():
    dfs = []
    for fname in sorted(os.listdir(INPUT_DIR)):
        if not fname.startswith('features_') or not fname.endswith('.csv'):
            continue
        # 파일명 'features_subject01.csv' -> subj 'subject01'
        subj = fname[len('features_'):-len('.csv')]
        df = pd.read_csv(os.path.join(INPUT_DIR, fname))
        df['subject'] = subj
        dfs.append(df)
    if not dfs:
        raise FileNotFoundError(f"No feature CSV files found in {INPUT_DIR}")
    return pd.concat(dfs, ignore_index=True)

# ——— 2) Leave-One-Subject-Out CV ———
def evaluate_loso(X, y, groups, model):
    logo = LeaveOneGroupOut()
    return cross_val_score(model, X, y, groups=groups, cv=logo, scoring='accuracy')

# ——— 3) 하이퍼파라미터 탐색 ———
def tune_random_forest(X, y, groups):
    param_grid = {
        'n_estimators': [50, 100, 200],
        'max_depth': [None, 5, 10],
    }
    rf = RandomForestClassifier(random_state=0)
    logo = LeaveOneGroupOut()
    grid = GridSearchCV(rf, param_grid, cv=logo, scoring='accuracy', n_jobs=-1)
    grid.fit(X, y, groups=groups)
    return grid

# ——— 4) 실행부 ———
if __name__ == '__main__':
    df_all = load_all_features()
    X = df_all.drop(columns=['label', 'subject']).values
    y = df_all['label'].values
    groups = df_all['subject'].values

    # 기본 평가
    print('== Leave-One-Subject-Out Evaluation ==')
    for name, clf in [
        ('k-NN (k=3)', KNeighborsClassifier(3)),
        ('RF (100 trees)', RandomForestClassifier(100, random_state=0))
    ]:
        scores = evaluate_loso(X, y, groups, clf)
        print(f'{name}: {scores.mean():.2f} ± {scores.std():.2f}')

    # RF 하이퍼파라미터 튜닝
    print('\n== RandomForest Hyperparameter Tuning (LOSO) ==')
    grid = tune_random_forest(X, y, groups)
    print('Best params:', grid.best_params_)
    print('Best LOSO accuracy:', grid.best_score_)

    # 결과 저장
    report = pd.DataFrame({
        'model': ['k-NN', 'RF'],
        'loso_accuracy': [
            evaluate_loso(X, y, groups, KNeighborsClassifier(3)).mean(),
            evaluate_loso(X, y, groups, RandomForestClassifier(100, random_state=0)).mean(),
        ]
    })
    out_path = os.path.join(RESULTS_DIR, 'loso_results.csv')
    report.to_csv(out_path, index=False)
    print('Saved LOSO results to', out_path)
