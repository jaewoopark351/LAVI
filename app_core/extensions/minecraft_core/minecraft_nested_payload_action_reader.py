#20260725_kpopmodder: Added nested action reader for GameCommandDTO payloads.
from __future__ import annotations

from typing import Any, Dict


class MinecraftNestedPayloadActionReader:
    def read(self, payload: Dict[str, Any]) -> Any:
        nested = payload.get("payload")
        if not isinstance(nested, dict):
            return None
        return (
            nested.get("action")
            or nested.get("type")
            or nested.get("event")
            or nested.get("event_type")
        )
