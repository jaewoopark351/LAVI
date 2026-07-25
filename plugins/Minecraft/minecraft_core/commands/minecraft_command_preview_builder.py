#20260725_kpopmodder: Added command previews so conversation replies can acknowledge before execution finishes.
from __future__ import annotations

from typing import Any, Dict, Mapping


class MinecraftCommandPreviewBuilder:
    REQUEST_EXCLUDED_KEYS = {
        "action",
        "event",
        "event_type",
        "type",
        "source",
        "raw_text",
        "language",
        "metadata",
    }

    def build(
        self,
        *,
        action: str,
        payload: Mapping[str, Any],
        supported: bool,
    ) -> Dict[str, Any]:
        request = self._request_from(payload)
        if not supported or not action:
            return {
                "ok": False,
                "accepted": False,
                "preview": True,
                "action": action,
                "request": request,
                "error": "unknown_action",
            }

        return {
            "ok": True,
            "accepted": True,
            "preview": True,
            "action": {
                "type": action,
                "request": request,
            },
            "request": request,
        }

    def _request_from(self, payload: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            key: value
            for key, value in dict(payload or {}).items()
            if key not in self.REQUEST_EXCLUDED_KEYS
        }
