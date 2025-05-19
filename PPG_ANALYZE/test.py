import keras, tensorflow as tf, os
print("keras backend:", keras.config.backend())  # 'tensorflow' 여야 정상
print("TF:", tf.__version__, "   Keras:", keras.__version__)
