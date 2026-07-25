#20260725_kpopmodder: Added item/count validator for Minecraft actions that target a counted item stack.
from __future__ import annotations

from typing import Any, Dict


class MinecraftItemCountActionValidator:
    def validate(self, action: str, item: Any, count: Any = 1) -> Dict[str, object]:
        item_name = str(item or "").strip()
        if not item_name:
            return {
                "ok": False,
                "action": action,
                "error": "missing_item",
                "message": "item is required.",
            }
        return {
            "ok": True,
            "item": item_name,
            "count": self._coerce_count(count),
        }

    def _coerce_count(self, value: Any) -> int:
        try:
            count = int(float(value))
        except (TypeError, ValueError):
            count = 1
        return max(1, count)
