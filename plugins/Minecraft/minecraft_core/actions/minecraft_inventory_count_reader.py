#20260725_kpopmodder: Added this reader to isolate item count lookup from inventory payloads.
from __future__ import annotations

from typing import Any, Dict


class MinecraftInventoryCountReader:
    def count(self, inventory_payload: Dict[str, Any], item: str) -> int:
        counts = inventory_payload.get("counts", {})
        if not isinstance(counts, dict):
            return 0

        raw_count = counts.get(item, 0)
        try:
            return max(0, int(raw_count))
        except (TypeError, ValueError):
            return 0
