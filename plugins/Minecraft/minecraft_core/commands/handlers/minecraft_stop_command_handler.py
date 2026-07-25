#20260725_kpopmodder: Added stop command handler to keep cancellation routing explicit.
from __future__ import annotations

from typing import Any, Dict


class MinecraftStopCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return result_formatter.with_action(action_service.stop(), action)
