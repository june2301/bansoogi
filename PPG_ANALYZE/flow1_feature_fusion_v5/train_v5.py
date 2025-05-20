import numpy as np, os, tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.utils import class_weight
from configs import BATCH, EPOCHS, PATIENCE
from model_v5 import build

D=np.load('flow1_v5_data.npz',allow_pickle=True)
raw, feat, y, subs = D['raw'], D['feat'], D['labels'], D['subjects']
# one‑hot
y_cat=tf.keras.utils.to_categorical(y)
# class weights
a,b=np.unique(y,return_counts=True); cw={i: max(b)/c for i,c in zip(a,b)}
logo=LeaveOneGroupOut(); accs=[]
for tr,te in logo.split(raw,y,groups=subs):
    m=build(raw.shape[1],feat.shape[1],y_cat.shape[1])
    es=tf.keras.callbacks.EarlyStopping('val_loss',patience=PATIENCE,restore_best_weights=True)
    rl=tf.keras.callbacks.ReduceLROnPlateau('val_loss',factor=0.5,patience=3)
    m.fit([raw[tr],feat[tr]], y_cat[tr], epochs=EPOCHS, batch_size=BATCH,
          validation_split=0.2, callbacks=[es,rl], class_weight=cw, verbose=0)
    _,a=m.evaluate([raw[te],feat[te]],y_cat[te],verbose=0); accs.append(a)
print(f'v5 LOSO Accuracy: {np.mean(accs):.2f} ± {np.std(accs):.2f}')

# final training
final=build(raw.shape[1],feat.shape[1],y_cat.shape[1])
final.fit([raw,feat],y_cat,epochs=EPOCHS, batch_size=BATCH,
          validation_split=0.2, callbacks=[es,rl], class_weight=cw, verbose=0)
# export
os.makedirs('models',exist_ok=True)
conv=tf.lite.TFLiteConverter.from_keras_model(final)
conv.optimizations=[tf.lite.Optimize.DEFAULT]
open('models/feature_fusion_v5.tflite','wb').write(conv.convert())
print('Saved models/feature_fusion_v5.tflite')