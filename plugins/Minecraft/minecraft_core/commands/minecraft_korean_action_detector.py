#20260725_kpopmodder: Added Korean action detection for Minecraft natural commands.
from __future__ import annotations

import re

from .minecraft_korean_equip_signal_detector import MinecraftKoreanEquipSignalDetector


class MinecraftKoreanActionDetector:
    COORDINATE_PATTERN = re.compile(r"-?\d+")
    STOP_WORDS = (
        "\uba48\ucdb0",
        "\uba48\ucd94",
        "\uc911\uc9c0",
        "\uc815\uc9c0",
        "\ucde8\uc18c",
        "\uce94\uc2ac",
    )
    INVENTORY_WORDS = ("\uc778\ubca4\ud1a0\ub9ac", "\uc778\ubca4", "\uac00\ubc29")
    STATUS_WORDS = ("\uc0c1\ud0dc",)
    HEALTH_WORDS = ("\uccb4\ub825", "\ud5ec\uc2a4")
    CURRENT_ACTION_WORDS = (
        "\ud604\uc7ac \ud589\ub3d9",
        "\ud604\uc7ac \uc561\uc158",
        "\ud604\uc7ac \uc791\uc5c5",
    )
    GET_WORDS = (
        "\uce90\uc640",
        "\uce90",
        "\uad6c\ud574",
        "\uac00\uc838\uc640",
        "\uac00\uc838",
        "\uc5bb\uc5b4",
        "\ubaa8\uc544",
        "\uc218\uc9d1",
    )
    CRAFT_WORDS = ("\ub9cc\ub4e4", "\uc81c\uc791", "\uc870\ud569")
    GOTO_WORDS = (
        "\uc774\ub3d9",
        "\uac00\uc918",
        "\uac00\ub77c",
        "\uac00",
        "\uc88c\ud45c",
    )

    def __init__(
        self,
        equip_signal_detector: MinecraftKoreanEquipSignalDetector | None = None,
    ):
        self.equip_signal_detector = (
            equip_signal_detector or MinecraftKoreanEquipSignalDetector()
        )

    def detect(self, text: object) -> str:
        lowered = str(text or "").strip().lower()
        if not lowered:
            return ""

        if self._contains_any(lowered, self.STOP_WORDS):
            return "stop"
        if self._contains_any(lowered, self.CURRENT_ACTION_WORDS):
            return "current_action"
        if self._contains_any(lowered, self.INVENTORY_WORDS):
            return "inventory"
        if self._contains_any(lowered, self.HEALTH_WORDS):
            return "health"
        if self._contains_any(lowered, self.STATUS_WORDS):
            return "status"

        has_get = self._contains_any(lowered, self.GET_WORDS)
        has_craft = self._contains_any(lowered, self.CRAFT_WORDS)
        has_equip = self._has_equip_signal(lowered)
        if (has_get or has_craft) and has_equip:
            return "get_and_equip"
        if has_craft:
            return "craft"
        if has_equip:
            return "equip"
        if self._contains_any(lowered, self.GOTO_WORDS) and self._has_coordinates(lowered):
            return "goto"
        if has_get:
            return "get_item"
        return ""

    def _contains_any(self, text: str, words: tuple[str, ...]) -> bool:
        return any(word in text for word in words)

    def _has_equip_signal(self, text: str) -> bool:
        return self.equip_signal_detector.has_signal(text)

    def _has_coordinates(self, text: str) -> bool:
        return len(self.COORDINATE_PATTERN.findall(text)) >= 3
