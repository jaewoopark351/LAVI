#20260725_kpopmodder: Added payload builder to isolate get-and-equip parser output shape.
from __future__ import annotations

from typing import Dict

from .minecraft_text_parse_helpers import build_payload


class MinecraftGetAndEquipPayloadBuilder:
    def build(self, raw_text: str, request: Dict[str, object]) -> Dict[str, object]:
        payload = build_payload("get_and_equip", raw_text)
        payload.update(request)
        return payload
