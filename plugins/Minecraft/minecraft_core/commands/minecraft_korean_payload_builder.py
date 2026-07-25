#20260725_kpopmodder: Added payload shaping for Korean Minecraft natural commands.
from __future__ import annotations

from typing import Dict

from .minecraft_text_parse_helpers import build_payload


class MinecraftKoreanPayloadBuilder:
    def build_simple(self, action: str, raw_text: str) -> Dict[str, object]:
        return self._payload(action, raw_text)

    def build_item(
        self,
        action: str,
        raw_text: str,
        item: str,
        count: int,
    ) -> Dict[str, object]:
        payload = self._payload(action, raw_text)
        payload["item"] = item
        if action != "equip":
            payload["count"] = count
        return payload

    def build_goto(self, raw_text: str, target: str) -> Dict[str, object]:
        payload = self._payload("goto", raw_text)
        payload["target"] = target
        return payload

    def _payload(self, action: str, raw_text: str) -> Dict[str, object]:
        payload = build_payload(action, raw_text)
        payload["language"] = "ko"
        return payload
