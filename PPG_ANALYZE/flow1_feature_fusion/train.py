import os, numpy as np, tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import StandardScaler, LabelEncoder

from configs import BATCH_SIZE, EPOCHS, LR, PATIENCE
from model import build

# --- Load NPZ ---
npz = np.load("flow1_data.npz", allow_pickle=True)
raw = npz["raw"]            # (N, 250)
Xf  = npz["features"]       # (N, F)
labels = npz["labels"]
subjects = npz["subjects"]

raw = raw.reshape(raw.shape[0], raw.shape[1], 1)
scaler = StandardScaler(); Xf = scaler.fit_transform(Xf)
le = LabelEncoder(); y = le.fit_transform(labels); y_cat = tf.keras.utils.to_categorical(y)

# --- LOSO ---
logo = LeaveOneGroupOut()
accs = []
for tr, te in logo.split(raw, y, groups=subjects):
    m = build(raw.shape[1], Xf.shape[1])
    m.compile(tf.keras.optimizers.Adam(LR), "categorical_crossentropy", ["accuracy"])
    es = tf.keras.callbacks.EarlyStopping(patience=PATIENCE, restore_best_weights=True, monitor="val_loss")
    m.fit([raw[tr], Xf[tr]], y_cat[tr], epochs=EPOCHS, batch_size=BATCH_SIZE, validation_split=0.2, verbose=0, callbacks=[es])
    _, a = m.evaluate([raw[te], Xf[te]], y_cat[te], verbose=0)
    accs.append(a)
print(f"LOSO={np.mean(accs):.2f}±{np.std(accs):.2f}")

# --- Final train & export ---
final = build(raw.shape[1], Xf.shape[1])
final.compile(tf.keras.optimizers.Adam(LR), "categorical_crossentropy", ["accuracy"])
final.fit([raw, Xf], y_cat, epochs=EPOCHS, batch_size=BATCH_SIZE, validation_split=0.2, verbose=0, callbacks=[es])

os.makedirs("models", exist_ok=True)
converter = tf.lite.TFLiteConverter.from_keras_model(final)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
open("models/feature_fusion.tflite", "wb").write(converter.convert())
print("★ Saved models/feature_fusion.tflite")