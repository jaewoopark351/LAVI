#20260725_kpopmodder: Added JSON formatter for Minecraft UI status payloads.
from __future__ import annotations

import json
from typing import Any, Dict


class MinecraftStatusJsonFormatter:
    def format(self, payload: Dict[str, Any]) -> str:
        return json.dumps(
            payload,
            ensure_ascii=False,
            indent=2,
            default=str,
        )
