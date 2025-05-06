# flatten_data.py
import shutil
from pathlib import Path
from tqdm import tqdm

def flatten(src_root: Path, dst_root: Path, pattern: str = "*"):
    """
    src_root 하위의 모든 파일 중 pattern 에 매치되는 것들을
    dst_root 폴더로 한 곳에 copy/move 합니다.
    """
    dst_root.mkdir(parents=True, exist_ok=True)
    files = list(src_root.rglob(pattern))
    for f in tqdm(files, desc=f"Flattening {src_root.name}", unit="file"):
        # 파일명 중복 방지: src 폴더 이름을 prefix 로 붙여도 좋습니다.
        new_name = f.name
        # 예: uuid_timestamp.dat → uuid_timestamp.dat
        shutil.copy(f, dst_root / new_name)
    print(f"[✓] {src_root} → {dst_root}: {len(files)} files")

if __name__ == "__main__":
    # 프로젝트 루트 기준 경로
    exs_src = Path("data/exs_raw")
    exs_dst = Path("data/exs_raw_flat")
    cap_src = Path("data/cap_raw")
    cap_dst = Path("data/cap_raw_flat")

    # ExtraSensory .dat 파일 평탄화
    flatten(exs_src, exs_dst, pattern="*.dat")
    # CAPTURE-24 .csv 파일 평탄화
    flatten(cap_src, cap_dst, pattern="*.csv")
