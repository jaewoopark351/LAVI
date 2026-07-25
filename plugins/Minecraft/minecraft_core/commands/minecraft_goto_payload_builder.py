#20260725_kpopmodder: Added payload builder to isolate goto parser output shape.
from __future__ import annotations

from typing import Dict

from .minecraft_text_parse_helpers import build_payload


class MinecraftGotoPayloadBuilder:
    def build(self, raw_text: str, target: str) -> Dict[str, object]:
        payload = build_payload("goto", raw_text)
        payload["target"] = target
        return payload
