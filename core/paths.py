#20260716_kpopmodder: Centralize portable path and config-directory handling for LAVI.
from __future__ import annotations

import os
import shutil
from pathlib import Path


class LaviPaths:
    #20260716_kpopmodder: Keep all repository-relative paths anchored to the project root, not cwd.
    def __init__(self, project_root: Path | str | None = None):
        self.project_root = Path(
            project_root or Path(__file__).resolve().parents[1]
        ).resolve()

    @property
    def config_dir(self) -> Path:
        override = str(os.environ.get("LAVI_CONFIG_DIR", "") or "").strip().strip("\"'")
        if override:
            path = Path(os.path.expandvars(os.path.expanduser(override)))
            if not path.is_absolute():
                path = self.project_root / path
            return path.resolve()
        return self.project_root / "config"

    def config_path(self, filename: str) -> Path:
        return self.config_dir / filename

    def root_path(self, *parts: str) -> Path:
        return self.project_root.joinpath(*parts)

    def resolve_path(self, value: str | os.PathLike[str] | None) -> Path | None:
        text = str(value or "").strip().strip("\"'")
        if not text:
            return None
        path = Path(os.path.expandvars(os.path.expanduser(text)))
        if path.is_absolute():
            return path.resolve()
        return (self.project_root / path).resolve()

    def to_storable_path(self, value: str | os.PathLike[str] | None) -> str:
        resolved = self.resolve_path(value)
        if resolved is None:
            return ""
        try:
            return str(resolved.relative_to(self.project_root))
        except ValueError:
            return str(resolved)

    def copy_example_if_missing(self, example_path: Path, target_path: Path) -> bool:
        if target_path.exists():
            return False
        target_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(example_path, target_path)
        return True


def get_lavi_paths(project_root: Path | str | None = None) -> LaviPaths:
    return LaviPaths(project_root)
