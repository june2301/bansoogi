"""End-to-End Conv1D+LSTM Hybrid Model"""
import tensorflow as tf
from configs import LR  # learning rate setting

def build_model(raw_len: int, feat_dim: int, num_classes: int = 3):
    # Raw input branch
    inp_raw = tf.keras.Input((raw_len, 1), name='raw')
    x = tf.keras.layers.Conv1D(32, 5, activation='relu')(inp_raw)
    x = tf.keras.layers.Conv1D(32, 5, activation='relu')(x)
    x = tf.keras.layers.LSTM(64)(x)

    # Feature input branch
    inp_feat = tf.keras.Input((feat_dim,), name='feat')
    y = tf.keras.layers.Dense(64, activation='relu')(inp_feat)

    # Fusion
    z = tf.keras.layers.concatenate([x, y])
    z = tf.keras.layers.Dense(64, activation='relu')(z)
    out = tf.keras.layers.Dense(num_classes, activation='softmax')(z)

    model = tf.keras.Model([inp_raw, inp_feat], out)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=LR),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    return model