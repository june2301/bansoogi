# -*- coding: utf-8 -*-
import tensorflow as tf

def build(raw_len: int, feat_dim: int, n_cls: int = 3):
    raw_in = tf.keras.Input((raw_len,1), name="raw")
    x = tf.keras.layers.Conv1D(64,5,activation="relu")(raw_in)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Conv1D(64,5,activation="relu")(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Conv1D(64,5,activation="relu")(x)
    x = tf.keras.layers.GlobalAveragePooling1D()(x)

    feat_in = tf.keras.Input((feat_dim,), name="feat")
    y = tf.keras.layers.Dense(64,activation="relu")(feat_in)
    y = tf.keras.layers.Dropout(0.3)(y)

    z = tf.keras.layers.concatenate([x,y])
    z = tf.keras.layers.Dense(64,activation="relu")(z)
    out = tf.keras.layers.Dense(n_cls,activation="softmax")(z)
    model=tf.keras.Model([raw_in,feat_in],[out])
    model.compile(tf.keras.optimizers.Adam(), "categorical_crossentropy", ["accuracy"])
    return model
