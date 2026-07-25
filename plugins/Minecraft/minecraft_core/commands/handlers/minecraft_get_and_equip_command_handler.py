#20260725_kpopmodder: Added get-and-equip command handler to keep compound action routing focused.
from __future__ import annotations

from typing import Any, Dict


class MinecraftGetAndEquipCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return result_formatter.with_action(
            action_service.get_and_equip(
                payload.get("item"),
                payload.get("count", 1),
            ),
            "get_and_equip",
        )
