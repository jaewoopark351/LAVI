#20260703_kpopmodder: Clean or verify Python bytecode before packaging LAV.
import argparse
import shutil
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
EXCLUDED_DIR_NAMES = {
    ".git",
    ".venv",
    "venv",
    "gradio-env",
    "installer_files",
    "BuildTools",
    "models",
    ".cache",
}


def iter_bytecode_paths(root):
    for path in root.rglob("__pycache__"):
        if path.is_dir() and not is_excluded(path, root):
            yield path
    for path in root.rglob("*.pyc"):
        if path.is_file() and not is_excluded(path, root):
            yield path


def is_excluded(path, root):
    try:
        parts = {part.lower() for part in path.relative_to(root).parts}
    except ValueError:
        return True
    return bool(parts & {name.lower() for name in EXCLUDED_DIR_NAMES})


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Remove or check Python bytecode/cache files.",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail if bytecode/cache files are present.",
    )
    args = parser.parse_args(argv)

    paths = sorted(
        iter_bytecode_paths(PROJECT_ROOT),
        key=lambda item: len(str(item)),
        reverse=True,
    )
    if args.check:
        for path in paths:
            print(path.relative_to(PROJECT_ROOT))
        return 1 if paths else 0

    removed = 0
    for path in paths:
        if not path.exists():
            continue
        if path.is_dir():
            shutil.rmtree(path)
        else:
            path.unlink()
        removed += 1

    print(f"Removed {removed} bytecode/cache paths.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
