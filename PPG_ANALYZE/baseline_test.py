import os
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import LeaveOneGroupOut, cross_val_score

# --------------------------------------------------
# Path settings
# --------------------------------------------------
ROOT_DIR   = os.path.dirname(__file__)
INPUT_DIR  = os.path.join(ROOT_DIR, 'outputs')
MODELS_DIR = os.path.join(ROOT_DIR, 'models')
TFLITE_MODEL_PATH = os.path.join(MODELS_DIR, 'ppg_classifier.tflite')

os.makedirs(MODELS_DIR, exist_ok=True)

# --------------------------------------------------
# 1) Load CSVs & add `subject` column
# --------------------------------------------------

def load_all_features():
    dfs = []
    for fname in sorted(os.listdir(INPUT_DIR)):
        if fname.startswith('features_') and fname.endswith('.csv'):
            df = pd.read_csv(os.path.join(INPUT_DIR, fname))
            df['subject'] = fname[len('features_'):-4]  # strip prefix / suffix
            dfs.append(df)
    if not dfs:
        raise FileNotFoundError(f'No CSVs found in {INPUT_DIR}')
    return pd.concat(dfs, ignore_index=True)



def prepare_data():
    df = load_all_features()
    X = df.drop(columns=['label', 'subject']).astype('float32').values
    y_raw = df['label'].values
    groups = df['subject'].values
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)  # 명시적 정규화
    le = LabelEncoder()
    y_int = le.fit_transform(y_raw)
    y_onehot = tf.keras.utils.to_categorical(y_int, num_classes=len(le.classes_))
    return X_scaled, y_onehot, groups, le

X, y_onehot, groups, le = prepare_data()
y_int = np.argmax(y_onehot, axis=1)
clf = RandomForestClassifier(n_estimators=100, random_state=0)
scores = cross_val_score(clf, X, y_int, cv=LeaveOneGroupOut().split(X, y_int, groups))
print(f"Random Forest LOSO: {scores.mean():.2f} ± {scores.std():.2f}")
