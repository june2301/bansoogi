import tensorflow as tf
from configs import LR

def se_gate(x, ratio=8):
    ch=x.shape[-1]
    s=tf.keras.layers.Dense(ch//ratio,activation='relu')(x)
    s=tf.keras.layers.Dense(ch,activation='sigmoid')(s)
    return tf.keras.layers.multiply([x,s])

def build(raw_len:int, feat_dim:int, n_cls:int):
    # raw branch
    in_r=tf.keras.Input((raw_len,1),name='raw')
    r=tf.keras.layers.Conv1D(32,5,activation='relu',padding='same')(in_r)
    r=tf.keras.layers.BatchNormalization()(r)
    r=tf.keras.layers.Conv1D(32,5,activation='relu',padding='same')(r)
    r=tf.keras.layers.GlobalAveragePooling1D()(r)

    # feat branch
    in_f=tf.keras.Input((feat_dim,),name='feat')
    f=tf.keras.layers.BatchNormalization()(in_f)
    f=tf.keras.layers.Dense(64,activation='relu')(f)
    f=tf.keras.layers.Dropout(0.3)(f)

    # fusion + SE gate
    z=tf.keras.layers.concatenate([r,f])
    z=tf.keras.layers.BatchNormalization()(z)
    z=tf.keras.layers.Dense(128,activation='relu')(z)
    z=se_gate(z)
    z=tf.keras.layers.Dropout(0.3)(z)
    out=tf.keras.layers.Dense(n_cls,activation='softmax')(z)
    model=tf.keras.Model([in_r,in_f],out)
    model.compile(tf.keras.optimizers.Adam(LR),'categorical_crossentropy',['accuracy'])
    return model