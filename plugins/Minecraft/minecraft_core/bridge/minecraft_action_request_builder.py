#20260725_kpopmodder: Added bridge action payload builder separate from endpoint dispatch.
from __future__ import annotations

from typing import Any, Dict


class MinecraftActionRequestBuilder:
    def get_item(self, item: str, count: int = 1) -> Dict[str, Any]:
        return {"item": str(item or "").strip(), "count": int(count or 1)}

    def get_and_equip(self, item: str, count: int = 1) -> Dict[str, Any]:
        return {"item": str(item or "").strip(), "count": int(count or 1)}

    def equip(self, item: str) -> Dict[str, Any]:
        return {"item": str(item or "").strip()}

    def goto(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, Any]:
        payload: Dict[str, Any] = {}
        if target is not None:
            payload["target"] = str(target or "").strip()
        for key, value in {
            "x": x,
            "y": y,
            "z": z,
            "dimension": dimension,
        }.items():
            if value is not None and str(value).strip():
                payload[key] = value
        return payload
