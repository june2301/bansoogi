import tensorflow as tf


def build(raw_len: int, feat_dim: int, num_classes: int = 3):
    # Raw branch
    inp_raw = tf.keras.layers.Input(shape=(raw_len, 1), name="raw")
    x = tf.keras.layers.Conv1D(32, 5, activation="relu")(inp_raw)
    x = tf.keras.layers.Conv1D(32, 5, activation="relu")(x)
    x = tf.keras.layers.GlobalAveragePooling1D()(x)

    # Feature branch
    inp_feat = tf.keras.layers.Input(shape=(feat_dim,), name="feat")
    y = tf.keras.layers.Dense(64, activation="relu")(inp_feat)

    # Fusion
    z = tf.keras.layers.concatenate([x, y])
    z = tf.keras.layers.Dense(64, activation="relu")(z)
    out = tf.keras.layers.Dense(num_classes, activation="softmax")(z)

    model = tf.keras.Model(inputs=[inp_raw, inp_feat], outputs=out)
    model.compile(optimizer=tf.keras.optimizers.Adam(), loss="categorical_crossentropy", metrics=["accuracy"])
    return model