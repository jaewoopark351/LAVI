#20260716_kpopmodder: Preflight keeps startup failures explicit before run.bat launches the app.
from __future__ import annotations

import argparse
import importlib
import sys
from pathlib import Path


EXPECTED_PYTHON = (3, 14)
EXPECTED_TORCH_CUDA_TAG = "cu130"
SUPPORTED_PROFILES = ("Core", "Full")
SUPPORTED_ACCELERATORS = ("CPU", "cu130")
EXPECTED_TORCH_PACKAGES = {
    "torch": "2.13.0+cu130",
    "torchvision": "0.28.0+cu130",
    "torchaudio": "2.11.0+cu130",
}


def _normalize_choice(value: str, allowed: tuple[str, ...], label: str) -> str:
    for item in allowed:
        if value.lower() == item.lower():
            return item
    valid = ", ".join(allowed)
    raise argparse.ArgumentTypeError(f"invalid {label}: {value!r}; expected one of {valid}")


def _parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run LAVI startup preflight checks.")
    parser.add_argument(
        "--profile",
        default="Full",
        type=lambda value: _normalize_choice(value, SUPPORTED_PROFILES, "profile"),
        help="Install/runtime profile to validate.",
    )
    parser.add_argument(
        "--accelerator",
        default="cu130",
        type=lambda value: _normalize_choice(value, SUPPORTED_ACCELERATORS, "accelerator"),
        help="Accelerator wheel set to validate.",
    )
    parser.add_argument(
        "--offline",
        action="store_true",
        help="Document that this preflight must not perform network or model setup.",
    )
    return parser.parse_args(argv)


def _repo_root() -> Path:
    return Path(__file__).resolve().parents[1]


def _venv_python(root: Path) -> Path:
    return root / "venv" / "Scripts" / "python.exe"


def _error(message: str) -> None:
    print(f"[preflight] ERROR: {message}", file=sys.stderr)


def _info(message: str) -> None:
    print(f"[preflight] {message}")


def _version_tuple() -> tuple[int, int]:
    return sys.version_info.major, sys.version_info.minor


def _check_python_version(errors: list[str]) -> None:
    if _version_tuple() != EXPECTED_PYTHON:
        errors.append(
            "Python 3.14 is required; "
            f"current interpreter is {sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}"
        )


def _check_repo_venv(errors: list[str], root: Path) -> None:
    expected = _venv_python(root)
    current = Path(sys.executable).resolve()
    if not expected.exists():
        errors.append(f"missing repository venv interpreter: {expected}")
        return
    if current != expected.resolve():
        errors.append(
            "preflight must run with the repository venv interpreter; "
            f"current={current}, expected={expected}"
        )
    if Path(sys.prefix).resolve() != (root / "venv").resolve():
        errors.append(
            "sys.prefix is not the repository venv; "
            f"current={Path(sys.prefix).resolve()}, expected={(root / 'venv').resolve()}"
        )


def _check_import(module_name: str, errors: list[str]) -> object | None:
    try:
        return importlib.import_module(module_name)
    except Exception as exc:
        errors.append(f"cannot import {module_name}: {exc}")
        return None


def _check_torch(errors: list[str], accelerator: str) -> None:
    if accelerator == "CPU":
        _info("torch check skipped for Accelerator=CPU")
        return

    torch = _check_import("torch", errors)
    torchvision = _check_import("torchvision", errors)
    torchaudio = _check_import("torchaudio", errors)
    if torch is None:
        return

    torch_version = getattr(torch, "__version__", "")
    if EXPECTED_TORCH_CUDA_TAG not in torch_version:
        errors.append(
            f"torch must be a CUDA 13.0 wheel ({EXPECTED_TORCH_CUDA_TAG}); "
            f"current torch.__version__={torch_version!r}"
        )
    if torch_version != EXPECTED_TORCH_PACKAGES["torch"]:
        errors.append(
            f"torch version mismatch: expected {EXPECTED_TORCH_PACKAGES['torch']}, "
            f"current {torch_version}"
        )

    version_notes = [
        f"torch={torch_version}",
        f"torchvision={getattr(torchvision, '__version__', '<missing>') if torchvision else '<missing>'}",
        f"torchaudio={getattr(torchaudio, '__version__', '<missing>') if torchaudio else '<missing>'}",
    ]
    if torchvision and getattr(torchvision, "__version__", "") != EXPECTED_TORCH_PACKAGES["torchvision"]:
        errors.append(
            "torchvision version mismatch: "
            f"expected {EXPECTED_TORCH_PACKAGES['torchvision']}, "
            f"current {getattr(torchvision, '__version__', '')}"
        )
    if torchaudio and getattr(torchaudio, "__version__", "") != EXPECTED_TORCH_PACKAGES["torchaudio"]:
        errors.append(
            "torchaudio version mismatch: "
            f"expected {EXPECTED_TORCH_PACKAGES['torchaudio']}, "
            f"current {getattr(torchaudio, '__version__', '')}"
        )
    try:
        cuda_available = bool(torch.cuda.is_available())
        device_count = int(torch.cuda.device_count()) if cuda_available else 0
    except Exception as exc:
        errors.append(f"torch CUDA availability check failed: {exc}")
        return

    _info(", ".join(version_notes))
    _info(f"torch.cuda.is_available={cuda_available} device_count={device_count}")


def _check_required_runtime_imports(errors: list[str]) -> None:
    for module_name in (
        "gradio",
        "requests",
        "pydantic",
        "sounddevice",
        "openai",
    ):
        _check_import(module_name, errors)


def main(argv: list[str] | None = None) -> int:
    args = _parse_args(argv)
    root = _repo_root()
    errors: list[str] = []

    _info(f"repo_root={root}")
    _info(f"python={Path(sys.executable).resolve()}")
    _info(f"version={sys.version.split()[0]}")
    _info(f"profile={args.profile} accelerator={args.accelerator} offline={args.offline}")

    _check_python_version(errors)
    _check_repo_venv(errors, root)
    _check_required_runtime_imports(errors)
    _check_torch(errors, args.accelerator)

    if errors:
        for message in errors:
            _error(message)
        return 1

    _info("ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
