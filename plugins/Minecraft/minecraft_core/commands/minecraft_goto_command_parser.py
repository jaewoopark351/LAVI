#20260725_kpopmodder: Added parser for goto-style Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_goto_payload_builder import MinecraftGotoPayloadBuilder
from .minecraft_goto_target_extractor import MinecraftGotoTargetExtractor


class MinecraftGotoCommandParser:
    def __init__(
        self,
        target_extractor: MinecraftGotoTargetExtractor | None = None,
        payload_builder: MinecraftGotoPayloadBuilder | None = None,
    ):
        self.target_extractor = target_extractor or MinecraftGotoTargetExtractor()
        self.payload_builder = payload_builder or MinecraftGotoPayloadBuilder()

    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        target = self.target_extractor.extract(text, lowered)
        if not target:
            return None
        return self.payload_builder.build(text, target)
