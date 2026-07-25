#20260725_kpopmodder: Added read command handler so health/status/inventory routing is data-driven.
from __future__ import annotations

from typing import Any, Dict


class MinecraftReadCommandHandler:
    def __init__(self, method_name: str):
        self.method_name = method_name

    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        method = getattr(action_service, self.method_name)
        return result_formatter.with_action(method(), action)
