import os
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import LabelEncoder

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

# --------------------------------------------------
# 2) Prepare X, y, groups (with z‑score normalisation later)
# --------------------------------------------------

def prepare_data():
    df = load_all_features()
    X = df.drop(columns=['label', 'subject']).astype('float32').values
    y_raw = df['label'].values
    groups = df['subject'].values
    le = LabelEncoder()
    y_int = le.fit_transform(y_raw)
    y_onehot = tf.keras.utils.to_categorical(y_int, num_classes=len(le.classes_))
    return X, y_onehot, groups, le

# --------------------------------------------------
# 3) Build MLP (Normalization + BN + Dropout)
# --------------------------------------------------

def build_model(input_dim: int, num_classes: int):
    norm_layer = tf.keras.layers.Normalization(axis=-1)
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(input_dim,)),
        norm_layer,
        tf.keras.layers.Dense(64),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Activation('relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(32),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Activation('relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(num_classes, activation='softmax'),
    ])
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-4),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    return model, norm_layer

# --------------------------------------------------
# 4) LOSO evaluation (each fold adapted & validated)
# --------------------------------------------------

def loso_accuracy(X, y, groups):
    logo = LeaveOneGroupOut()
    accs = []
    for train_idx, test_idx in logo.split(X, y, groups):
        X_train, X_test = X[train_idx], X[test_idx]
        y_train, y_test = y[train_idx], y[test_idx]
        model, norm = build_model(X.shape[1], y.shape[1])
        norm.adapt(X_train)  # fit stats on training fold only
        es = tf.keras.callbacks.EarlyStopping(monitor='val_loss', patience=8, restore_best_weights=True)
        model.fit(X_train, y_train,
                  epochs=80,
                  batch_size=32,
                  validation_split=0.2,
                  callbacks=[es],
                  verbose=0)
        _, acc = model.evaluate(X_test, y_test, verbose=0)
        accs.append(acc)
    return np.mean(accs), np.std(accs)

# --------------------------------------------------
# 5) Train on full data + convert to TFLite
# --------------------------------------------------

def train_full_and_convert():
    X, y_onehot, groups, le = prepare_data()

    # 5‑fold LOSO benchmark
    mean_acc, std_acc = loso_accuracy(X, y_onehot, groups)
    print(f"LOSO (BN+Dropout, val_split 0.2) accuracy: {mean_acc:.2f} ± {std_acc:.2f}")

    # Train final model on all data
    model, norm = build_model(X.shape[1], y_onehot.shape[1])
    norm.adapt(X)
    es = tf.keras.callbacks.EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)
    model.fit(X, y_onehot,
              epochs=120,
              batch_size=32,
              validation_split=0.2,
              callbacks=[es],
              verbose=1)

    # Convert to TFLite (default optimisation)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    with open(TFLITE_MODEL_PATH, 'wb') as f:
        f.write(tflite_model)
    print('Saved optimised TFLite model →', TFLITE_MODEL_PATH)

if __name__ == '__main__':
    train_full_and_convert()
