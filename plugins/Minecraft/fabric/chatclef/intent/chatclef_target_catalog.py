#20260803_kpopmodder: Added ChatClef catalog whitelist loading for Korean targets.
from __future__ import annotations

import re
from pathlib import Path


class ChatClefTargetCatalog:
    _TARGET_RE = re.compile(r"^[a-z0-9_]+$")
    _BASELINE_TARGETS = {
        "diamond_pickaxe",
        "diamond_axe",
        "iron_shovel",
        "golden_axe",
        "netherite_sword",
        "gold_ingot",
        "iron_ingot",
        "cooked_beef",
        "log",
        "wooden_axe",
    }

    def __init__(self, path: Path | None = None):
        self.path = path or self.default_path()
        self.targets = self._load_targets(self.path)

    def contains(self, target: str) -> bool:
        return str(target or "") in self.targets

    @classmethod
    def default_path(cls) -> Path:
        current = Path(__file__).resolve()
        for parent in current.parents:
            candidate = (
                parent
                / "plugins"
                / "Minecraft"
                / "runtime"
                / "chatclef_fabric_1.20.1"
                / "CataloguedResources.txt"
            )
            if candidate.exists():
                return candidate
        return (
            current.parents[5]
            / "plugins"
            / "Minecraft"
            / "runtime"
            / "chatclef_fabric_1.20.1"
            / "CataloguedResources.txt"
        )

    def _load_targets(self, path: Path) -> frozenset[str]:
        if not path.exists():
            raise FileNotFoundError(f"ChatClef target catalog does not exist: {path}")
        raw_lines = path.read_text(encoding="utf-8").splitlines()
        targets: list[str] = []
        started = False
        for line_number, raw_line in enumerate(raw_lines, start=1):
            line = raw_line.strip()
            if not line:
                continue
            if not started and not self._TARGET_RE.fullmatch(line):
                continue
            started = True
            if not self._TARGET_RE.fullmatch(line):
                raise ValueError(
                    f"Invalid ChatClef target at {path}:{line_number}: {line}"
                )
            targets.append(line)
        if not targets:
            raise ValueError(f"ChatClef target catalog is empty: {path}")
        if len(targets) != len(set(targets)):
            raise ValueError(f"ChatClef target catalog contains duplicate targets: {path}")
        missing = sorted(self._BASELINE_TARGETS - set(targets))
        if missing:
            raise ValueError(
                "ChatClef target catalog is missing baseline targets: "
                + ", ".join(missing)
            )
        return frozenset(targets)
