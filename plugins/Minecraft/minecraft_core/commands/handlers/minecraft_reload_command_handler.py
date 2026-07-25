#20260725_kpopmodder: Added reload command handler so router does not special-case reload.
from __future__ import annotations

from typing import Any, Dict


class MinecraftReloadCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return action_service.reload()
