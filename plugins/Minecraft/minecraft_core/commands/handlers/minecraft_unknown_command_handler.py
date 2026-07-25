#20260725_kpopmodder: Added unknown command handler so unsupported action response shape is centralized.
from __future__ import annotations

from typing import Any, Dict


class MinecraftUnknownCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return {
            "ok": False,
            "action": action or "",
            "error": "unknown_action",
        }
