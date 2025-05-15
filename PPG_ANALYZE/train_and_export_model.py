import os
import pandas as pd
import numpy as np
import tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import LabelEncoder

# ——— 설정 ———
INPUT_DIR = os.path.join(os.path.dirname(__file__), 'outputs')
MODELS_DIR = os.path.join(os.path.dirname(__file__), 'models')
TFLITE_MODEL_PATH = os.path.join(MODELS_DIR, 'ppg_classifier.tflite')

os.makedirs(MODELS_DIR, exist_ok=True)

# ——— 1) 데이터 로드 ———
def load_all_features():
    dfs = []
    for fname in sorted(os.listdir(INPUT_DIR)):
        if not fname.startswith('features_') or not fname.endswith('.csv'):
            continue
        subj = fname[len('features_'):-len('.csv')]
        df = pd.read_csv(os.path.join(INPUT_DIR, fname))
        df['subject'] = subj
        dfs.append(df)
    if not dfs:
        raise FileNotFoundError(f"No feature CSV files found in {INPUT_DIR}")
    return pd.concat(dfs, ignore_index=True)

# ——— 2) 데이터 준비 ———
def prepare_data():
    df = load_all_features()
    # Features and labels
    X = df.drop(columns=['label', 'subject']).values.astype(np.float32)
    y_raw = df['label'].values
    # Encode labels
    le = LabelEncoder()
    y = le.fit_transform(y_raw)
    num_classes = len(le.classes_)
    y_onehot = tf.keras.utils.to_categorical(y, num_classes)
    # Groups for LOSO
    groups = df['subject'].values
    return X, y_onehot, groups, le

# ——— 3) LOSO 평가 ———
def evaluate_loso_keras(X, y_onehot, groups):
    logo = LeaveOneGroupOut()
    accuracies = []
    for train_idx, test_idx in logo.split(X, y_onehot, groups):
        model = build_model(input_dim=X.shape[1], num_classes=y_onehot.shape[1])
        model.fit(X[train_idx], y_onehot[train_idx], epochs=20, batch_size=16, verbose=0)
        loss, acc = model.evaluate(X[test_idx], y_onehot[test_idx], verbose=0)
        accuracies.append(acc)
    return np.mean(accuracies), np.std(accuracies)

# ——— 4) 모델 정의 ———
def build_model(input_dim, num_classes):
    model = tf.keras.Sequential([
        tf.keras.layers.InputLayer(input_shape=(input_dim,)),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(num_classes, activation='softmax'),
    ])
    model.compile(optimizer='adam',
                  loss='categorical_crossentropy',
                  metrics=['accuracy'])
    return model

# ——— 5) 최종 학습 및 TFLite 변환 ———
def train_and_convert():
    X, y_onehot, groups, le = prepare_data()
    # LOSO 평가
    mean_acc, std_acc = evaluate_loso_keras(X, y_onehot, groups)
    print(f"LOSO Keras MLP Accuracy: {mean_acc:.2f} ± {std_acc:.2f}")

    # 전체 데이터로 모델 학습
    model = build_model(X.shape[1], y_onehot.shape[1])
    model.fit(X, y_onehot, epochs=50, batch_size=16, verbose=1)

    # TFLite 변환 및 저장
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    with open(TFLITE_MODEL_PATH, 'wb') as f:
        f.write(tflite_model)
    print(f"Saved TFLite model to {TFLITE_MODEL_PATH}")

if __name__ == '__main__':
    train_and_convert()
