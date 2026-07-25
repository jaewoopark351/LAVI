#20260725_kpopmodder: Added Korean natural command parser for Minecraft actions.
from __future__ import annotations

from typing import Dict

from .minecraft_korean_action_detector import MinecraftKoreanActionDetector
from .minecraft_korean_count_parser import MinecraftKoreanCountParser
from .minecraft_korean_goto_target_extractor import MinecraftKoreanGotoTargetExtractor
from .minecraft_korean_item_dictionary import MinecraftKoreanItemDictionary
from .minecraft_korean_payload_builder import MinecraftKoreanPayloadBuilder


class MinecraftKoreanCommandParser:
    SIMPLE_ACTIONS = {"health", "status", "inventory", "current_action", "stop"}
    ITEM_ACTIONS = {"get_item", "get_and_equip", "craft", "equip"}

    def __init__(
        self,
        action_detector: MinecraftKoreanActionDetector | None = None,
        item_dictionary: MinecraftKoreanItemDictionary | None = None,
        count_parser: MinecraftKoreanCountParser | None = None,
        goto_target_extractor: MinecraftKoreanGotoTargetExtractor | None = None,
        payload_builder: MinecraftKoreanPayloadBuilder | None = None,
    ):
        self.action_detector = action_detector or MinecraftKoreanActionDetector()
        self.item_dictionary = item_dictionary or MinecraftKoreanItemDictionary()
        self.count_parser = count_parser or MinecraftKoreanCountParser()
        self.goto_target_extractor = (
            goto_target_extractor or MinecraftKoreanGotoTargetExtractor()
        )
        self.payload_builder = payload_builder or MinecraftKoreanPayloadBuilder()

    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        action = self.action_detector.detect(text)
        if not action:
            return None

        if action in self.SIMPLE_ACTIONS:
            return self.payload_builder.build_simple(action, text)
        if action == "goto":
            return self._parse_goto(text)
        if action in self.ITEM_ACTIONS:
            return self._parse_item_action(action, text)
        return None

    def _parse_goto(self, text: str) -> Dict[str, object] | None:
        target = self.goto_target_extractor.extract(text)
        if not target:
            return None
        return self.payload_builder.build_goto(text, target)

    def _parse_item_action(
        self,
        action: str,
        text: str,
    ) -> Dict[str, object] | None:
        item = self.item_dictionary.find_item(text)
        if not item:
            return None

        count = self.count_parser.parse(text, default=1)
        return self.payload_builder.build_item(action, text, item, count)
