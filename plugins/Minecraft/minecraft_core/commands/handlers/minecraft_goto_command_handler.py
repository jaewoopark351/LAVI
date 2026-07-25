#20260725_kpopmodder: Added goto command handler to keep movement routing out of router conditionals.
from __future__ import annotations

from typing import Any, Dict


class MinecraftGotoCommandHandler:
    def handle(
        self,
        action: str,
        payload: Dict[str, Any],
        action_service,
        result_formatter,
    ) -> Dict[str, Any]:
        return result_formatter.with_action(
            action_service.goto(
                payload.get("target"),
                x=payload.get("x"),
                y=payload.get("y"),
                z=payload.get("z"),
                dimension=payload.get("dimension"),
            ),
            "goto",
        )
