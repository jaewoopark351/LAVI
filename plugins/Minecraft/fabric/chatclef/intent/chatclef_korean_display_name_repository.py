#20260819_kpopmodder: Keep Korean input aliases separate from canonical response display names.
from __future__ import annotations

import json
from pathlib import Path
from typing import Any


class ChatClefKoreanDisplayNameRepository:
    def __init__(self, resource_dir: Path | None = None):
        self._resource_dir = resource_dir or Path(__file__).resolve().parent / "resources"
        self.display_names = self._load("korean_item_display_names.json")

    def display_name(self, target: object, fallback: str = "") -> str:
        target_text = str(target or "").strip()
        if not target_text:
            return str(fallback or "")
        return self.display_names.get(target_text, str(fallback or target_text))

    def _load(self, file_name: str) -> dict[str, str]:
        path = self._resource_dir / file_name
        with path.open("r", encoding="utf-8") as handle:
            payload: Any = json.load(handle)
        if not isinstance(payload, dict):
            raise ValueError(f"{file_name} must contain a JSON object")
        return {str(target): str(display) for target, display in payload.items()}
