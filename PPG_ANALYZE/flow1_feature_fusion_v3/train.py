import os, numpy as np, tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import StandardScaler, LabelEncoder
from configs import BATCH_SIZE, EPOCHS, INIT_LR, PATIENCE
from model import build

# Load augmented data
npz = np.load('flow1_v3_data.npz',allow_pickle=True)
raw, feat = npz['raw'], npz['feat']
labels, subs = npz['labels'], npz['subjects']

# Preprocess
scaler = StandardScaler(); feat = scaler.fit_transform(feat)
le = LabelEncoder(); y = le.fit_transform(labels);
y_cat = tf.keras.utils.to_categorical(y)
# class weights
u,c = np.unique(y, return_counts=True); cw = {i: max(c)/cnt for i,cnt in zip(u,c)}

# LOSO
logo = LeaveOneGroupOut(); accs=[]
for tr,te in logo.split(raw,y,groups=subs):
    m = build(raw.shape[1], feat.shape[1])
    lr_sched = tf.keras.callbacks.ReduceLROnPlateau(monitor='val_loss',factor=0.5,patience=3,min_lr=5e-6)
    es = tf.keras.callbacks.EarlyStopping(monitor='val_loss',patience=PATIENCE,restore_best_weights=True)
    m.compile(tf.keras.optimizers.Adam(INIT_LR),'categorical_crossentropy',['accuracy'])
    m.fit([raw[tr],feat[tr]],y_cat[tr],batch_size=BATCH_SIZE,epochs=EPOCHS,
          validation_split=0.2,callbacks=[lr_sched,es],class_weight=cw,verbose=0)
    _,a = m.evaluate([raw[te],feat[te]],y_cat[te],verbose=0)
    accs.append(a)
print(f'LOSO {np.mean(accs):.2f}±{np.std(accs):.2f}')

# Final train & TFLite
def final_export():
    m = build(raw.shape[1],feat.shape[1])
    m.compile(tf.keras.optimizers.Adam(INIT_LR),'categorical_crossentropy',['accuracy'])
    m.fit([raw,feat],y_cat,batch_size=BATCH_SIZE,epochs=EPOCHS,
          validation_split=0.2,callbacks=[lr_sched,es],class_weight=cw,verbose=0)
    os.makedirs('models',exist_ok=True)
    tfl = tf.lite.TFLiteConverter.from_keras_model(m)
    tfl.optimizations = [tf.lite.Optimize.DEFAULT]
    open('models/feature_fusion_v3.tflite','wb').write(tfl.convert())
    print('Saved models/feature_fusion_v3.tflite')

if __name__=='__main__': final_export()