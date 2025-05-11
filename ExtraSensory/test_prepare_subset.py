# test_prepare_subset.py
import numpy as np
from itertools import islice
from pathlib import Path

import src.watch_har_pipeline as pipeline

# -------------------------------------------------------------------
# 1) monkey-patch로 subset만 읽도록 감싸기
# -------------------------------------------------------------------
orig_exs = pipeline.iter_exs_dat
orig_cap = pipeline.iter_cap_csv

def iter_exs_small(root: Path):
    """
    ExtraSensory 데이터 중 첫 6 윈도우 소스(파일 레벨로 추정)만 읽습니다.
    실제 파일 개수에 맞게 islice 개수를 조절하세요.
    """
    yield from islice(orig_exs(root), 6000)

def iter_cap_small(root: Path):
    """
    CAPTURE-24 데이터 중 첫 6 윈도우 소스만 읽습니다.
    """
    yield from islice(orig_cap(root), 2)

pipeline.iter_exs_dat = iter_exs_small
pipeline.iter_cap_csv = iter_cap_small

# -------------------------------------------------------------------
# 2) subset 테스트 실행
# -------------------------------------------------------------------
if __name__ == "__main__":
    print(">>> Running prepare_npz() on a small subset of data ...")
    try:
        pipeline.prepare_npz()
    except RuntimeError as e:
        # 라벨된 윈도우가 전혀 없을 때 발생하는 guard 메시지
        print("[RuntimeError]", e)
        exit(1)

    # npz 로딩 확인
    data = np.load(pipeline.NPZ_PATH)
    X, y = data["X"], data["y"]
    print(f"\n[Test result] X.shape = {X.shape}")
    print(f"[Test result] y distribution = {np.bincount(y)}")
