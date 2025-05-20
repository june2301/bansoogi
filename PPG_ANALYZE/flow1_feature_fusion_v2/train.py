# -*- coding: utf-8 -*-
import os, numpy as np, tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import StandardScaler, LabelEncoder
from configs import BATCH_SIZE,EPOCHS,INIT_LR,PATIENCE
from model import build

data=np.load("flow1_v2_data.npz",allow_pickle=True)
raw=data["raw"]; feat=data["feat"]; labels=data["labels"]; subjects=data["subjects"]

scaler=StandardScaler(); feat=scaler.fit_transform(feat)
le=LabelEncoder(); y=le.fit_transform(labels); ycat=tf.keras.utils.to_categorical(y)

# class weight (standing/sitting/lying roughly balanced? compute)
unique, counts = np.unique(y, return_counts=True)
cw={i:max(counts)/c for i,c in zip(unique,counts)}

logo=LeaveOneGroupOut(); accs=[]
for tr,te in logo.split(raw,y,groups=subjects):
    mdl=build(raw.shape[1], feat.shape[1])
    lr_sched=tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss",factor=0.5,
                                                  patience=3,min_lr=5e-6,verbose=0)
    es=tf.keras.callbacks.EarlyStopping(patience=PATIENCE, restore_best_weights=True)
    mdl.compile(tf.keras.optimizers.Adam(INIT_LR),"categorical_crossentropy",["accuracy"])
    mdl.fit([raw[tr],feat[tr]],ycat[tr],
            batch_size=BATCH_SIZE,epochs=EPOCHS,validation_split=0.2,
            callbacks=[lr_sched,es],class_weight=cw,verbose=0)
    _,a=mdl.evaluate([raw[te],feat[te]],ycat[te],verbose=0)
    accs.append(a)
print("LOSO {:.2f}±{:.2f}".format(np.mean(accs),np.std(accs)))

# final train all & export
m_final=build(raw.shape[1],feat.shape[1])
m_final.compile(tf.keras.optimizers.Adam(INIT_LR),"categorical_crossentropy",["accuracy"])
m_final.fit(
    [raw, feat], ycat,
    batch_size=BATCH_SIZE, epochs=EPOCHS,
    validation_split=0.2,  # ← 추가
    callbacks=[lr_sched, es],
    class_weight=cw, verbose=0
)
os.makedirs("models",exist_ok=True)
conv=tf.lite.TFLiteConverter.from_keras_model(m_final)
conv.optimizations=[tf.lite.Optimize.DEFAULT]
open("models/feature_fusion_v2.tflite","wb").write(conv.convert())
print("Saved models/feature_fusion_v2.tflite")
