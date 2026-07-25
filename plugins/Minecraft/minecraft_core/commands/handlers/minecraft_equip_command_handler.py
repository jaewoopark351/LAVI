#20260725_kpopmodder: Added equip command handler to keep hand item routing out of router conditionals.
from __future__ import annotations

from typing import Any, Dict


class MinecraftEquipCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return result_formatter.with_action(
            action_service.equip(payload.get("item")),
            "equip",
        )
