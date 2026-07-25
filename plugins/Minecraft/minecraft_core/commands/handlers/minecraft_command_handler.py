#20260725_kpopmodder: Added command handler protocol for Minecraft command routing.
from __future__ import annotations

from typing import Any, Dict, Protocol


class MinecraftCommandHandler(Protocol):
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        ...
