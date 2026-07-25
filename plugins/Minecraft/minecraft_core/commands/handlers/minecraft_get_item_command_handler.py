#20260725_kpopmodder: Added get-item command handler to keep action routing out of router conditionals.
from __future__ import annotations

from typing import Any, Dict


class MinecraftGetItemCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return result_formatter.with_action(
            action_service.get_item(
                payload.get("item"),
                payload.get("count", 1),
            ),
            "get_item",
        )
