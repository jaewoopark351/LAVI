#20260819_kpopmodder: Adapt one trusted bridge last-result DTO shape for canonical validation.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftChatClefBridgeResultAdapter:
    def adapt(self, last_result: Mapping[str, Any]) -> dict[str, Any]:
        nested = dict(last_result)
        payload: dict[str, Any] = {"status": nested}
        mirror_fields = {
            "request_id": "request_id",
            "ok": "ok",
            "error_code": "error",
            "message": "message",
            "data": "details",
        }
        for nested_name, outer_name in mirror_fields.items():
            if nested_name in nested:
                payload[outer_name] = nested[nested_name]
        return payload
