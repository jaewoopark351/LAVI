#20260803_kpopmodder: Added resource-backed Korean alias ownership for ChatClef.
from __future__ import annotations

import json
from pathlib import Path
from typing import Any


class ChatClefKoreanAliasRepository:
    def __init__(self, resource_dir: Path | None = None):
        self._resource_dir = resource_dir or Path(__file__).resolve().parent / "resources"
        self.material_aliases = self._load("korean_material_aliases.json")
        self.equipment_aliases = self._load("korean_equipment_aliases.json")
        self.fixed_item_aliases = self._load("korean_item_aliases.json")

    def sorted_aliases(self, aliases: dict[str, str]) -> list[tuple[str, str]]:
        return sorted(aliases.items(), key=lambda item: len(item[0]), reverse=True)

    def _load(self, file_name: str) -> dict[str, str]:
        path = self._resource_dir / file_name
        with path.open("r", encoding="utf-8") as handle:
            payload: Any = json.load(handle)
        if not isinstance(payload, dict):
            raise ValueError(f"{file_name} must contain a JSON object")
        return {str(alias): str(target) for alias, target in payload.items()}
