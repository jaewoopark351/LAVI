#20260725_kpopmodder: Added craft command handler to keep action routing out of router conditionals.
from __future__ import annotations

from typing import Any, Dict


class MinecraftCraftCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return result_formatter.with_action(
            action_service.craft(
                payload.get("item"),
                payload.get("count", 1),
            ),
            "craft",
        )
