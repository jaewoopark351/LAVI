#20260725_kpopmodder: Added nested payload flattener so command payload shape handling stays local.
from __future__ import annotations

from typing import Any, Dict


class MinecraftNestedPayloadFlattener:
    def flatten(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        nested = payload.get("payload")
        if not isinstance(nested, dict):
            return payload

        merged = dict(nested)
        merged.update({key: value for key, value in payload.items() if key != "payload"})
        return merged
