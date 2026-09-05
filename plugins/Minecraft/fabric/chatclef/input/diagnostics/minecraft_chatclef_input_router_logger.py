#20260905_kpopmodder: Isolate bounded router diagnostics from routing decisions.
from __future__ import annotations

import json
from typing import Any


class MinecraftChatClefInputRouterLogger:
    def __init__(self, log_callback):
        self._log_callback = log_callback

    def log(self, message: str) -> None:
        try:
            self._log_callback(f"[MinecraftChatClefInputRouter] {message}")
        except Exception:
            pass

    def log_active_command_reconciliation(
        self,
        details: dict[str, Any],
    ) -> None:
        diagnostic = details.get("active_command_reconciliation")
        if not isinstance(diagnostic, dict):
            return
        self.log(
            "active command reconciliation diagnostic "
            f"{self._compact_json(diagnostic)}"
        )

    def _compact_json(self, payload: Any) -> str:
        try:
            return json.dumps(
                payload,
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            )
        except Exception as error:
            return f"<json failed {type(error).__name__}: {error}>"


__all__ = ("MinecraftChatClefInputRouterLogger",)
