#20260818_kpopmodder: Resolve one exact dedicated-world playerdata file fail closed.
from __future__ import annotations

from collections.abc import Mapping
from pathlib import Path


def resolve_player_data_path(
    environment: Mapping[str, object],
) -> tuple[Path | None, str]:
    log_dir_text = _text(environment.get("log_dir"))
    expected_instance = _text(environment.get("expected_instance"))
    expected_world = _text(environment.get("expected_world"))
    if not log_dir_text:
        return None, "instance log directory is required for gameplay observation"
    if not _is_single_component(expected_instance):
        return None, "expected instance must be one path component"
    if not _is_single_component(expected_world):
        return None, "expected world must be one path component"

    log_dir = Path(log_dir_text).resolve()
    instance_dir = log_dir.parent
    if log_dir.name.lower() != "logs":
        return None, "instance log directory must end in logs"
    if instance_dir.name != expected_instance:
        return None, "playerdata instance does not match the expected instance"

    saves_root = (instance_dir / "saves").resolve()
    world_dir = (saves_root / expected_world).resolve()
    if not world_dir.is_relative_to(saves_root):
        return None, "expected world escaped the instance saves directory"
    player_data_dir = world_dir / "playerdata"
    try:
        candidates = sorted(
            path.resolve()
            for path in player_data_dir.glob("*.dat")
            if path.is_file()
        )
    except OSError as error:
        return None, f"playerdata directory is inaccessible: {type(error).__name__}"
    if len(candidates) != 1:
        return None, f"expected exactly one playerdata file, observed {len(candidates)}"
    if not candidates[0].is_relative_to(player_data_dir.resolve()):
        return None, "playerdata file escaped the expected world"
    return candidates[0], ""


def _is_single_component(value: str) -> bool:
    return bool(value) and value not in {".", ".."} and not any(
        separator in value for separator in ("/", "\\")
    )


def _text(value: object) -> str:
    return str(value or "").strip()
