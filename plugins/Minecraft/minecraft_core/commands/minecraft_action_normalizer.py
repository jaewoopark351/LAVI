#20260725_kpopmodder: Added action normalizer so command action aliases share one normalization rule.
from __future__ import annotations

from typing import Any, Dict


class MinecraftActionNormalizer:
    def normalize_value(self, value: Any) -> str:
        return str(value or "").strip().lower().replace("-", "_")

    def action_from(self, payload: Dict[str, Any]) -> str:
        return self.normalize_value(
            payload.get("action")
            or payload.get("type")
            or payload.get("event")
            or payload.get("event_type")
        )
