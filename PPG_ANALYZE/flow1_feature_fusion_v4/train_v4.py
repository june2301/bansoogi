"""Train v4 Model with LOSO & TFLite Export"""
import numpy as np, tensorflow as tf, os
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import StandardScaler, LabelEncoder
from configs import BATCH_SIZE, EPOCHS, LR, PATIENCE
from model_v4 import build_model

# Load data
data=np.load('flow1_v4_data.npz',allow_pickle=True)
raw = data['raw']; feat=data['feat']
labels, subs = data['labels'], data['subjects']

# Preprocess
scaler = StandardScaler(); feat = scaler.fit_transform(feat)
le = LabelEncoder(); y = le.fit_transform(labels); y_cat = tf.keras.utils.to_categorical(y)

# LOSO CV
logo = LeaveOneGroupOut()
accs=[]
for tr, te in logo.split(raw, y, groups=subs):
    m = build_model(raw.shape[1], feat.shape[1], num_classes=y_cat.shape[1])
    es = tf.keras.callbacks.EarlyStopping(monitor='val_loss', patience=PATIENCE, restore_best_weights=True)
    m.fit([raw[tr], feat[tr]], y_cat[tr], batch_size=BATCH_SIZE,
          epochs=EPOCHS, validation_split=0.2, callbacks=[es], verbose=0)
    _, a = m.evaluate([raw[te], feat[te]], y_cat[te], verbose=0)
    accs.append(a)
print(f"v4 LOSO Accuracy: {np.mean(accs):.2f} ± {np.std(accs):.2f}")

# Final train & export
final = build_model(raw.shape[1], feat.shape[1], num_classes=y_cat.shape[1])
final.fit([raw, feat], y_cat, batch_size=BATCH_SIZE,
          epochs=EPOCHS, validation_split=0.2, callbacks=[es], verbose=0)

os.makedirs('models', exist_ok=True)
converter = tf.lite.TFLiteConverter.from_keras_model(final)
converter.optimizations=[tf.lite.Optimize.DEFAULT]
open('models/feature_fusion_v4.tflite','wb').write(converter.convert())
print('Saved models/feature_fusion_v4.tflite')