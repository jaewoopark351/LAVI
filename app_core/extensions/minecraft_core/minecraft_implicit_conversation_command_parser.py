#20260725_kpopmodder: Added implicit parser for Minecraft-like chat when the bridge is active.
from __future__ import annotations

from .minecraft_conversation_command_parser import MinecraftConversationCommandParser
from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_unrelated_filter import MinecraftConversationUnrelatedFilter
from .minecraft_korean_command_signal_detector import MinecraftKoreanCommandSignalDetector


class MinecraftImplicitConversationCommandParser:
    def __init__(
        self,
        korean_signal_detector: MinecraftKoreanCommandSignalDetector | None = None,
        unrelated_filter: MinecraftConversationUnrelatedFilter | None = None,
    ):
        self.korean_signal_detector = (
            korean_signal_detector or MinecraftKoreanCommandSignalDetector()
        )
        self.unrelated_filter = unrelated_filter or MinecraftConversationUnrelatedFilter()

    def parse(self, text: object) -> MinecraftConversationCommandRoute | None:
        raw_text = str(text or "").strip()
        if not raw_text:
            return None
        if self.unrelated_filter.is_unrelated(raw_text):
            return None

        command = self._clean_command(raw_text)
        if self._looks_like_implicit_command(command):
            return MinecraftConversationCommandRoute(
                game="minecraft",
                command=command,
                raw_text=raw_text,
                trigger="implicit_active",
            )
        return None

    def _looks_like_implicit_command(self, command: str) -> bool:
        lowered = command.lower()
        if self._looks_like_direct_english_command(lowered):
            return True
        if self._looks_like_direct_item_count(lowered):
            return True
        return self.korean_signal_detector.looks_like_command(command)

    def _looks_like_direct_english_command(self, lowered: str) -> bool:
        return any(
            lowered == prefix.strip() or lowered.startswith(prefix)
            for prefix in MinecraftConversationCommandParser.COMMAND_PREFIXES
        )

    def _looks_like_direct_item_count(self, lowered: str) -> bool:
        return bool(
            MinecraftConversationCommandParser.DIRECT_ITEM_PATTERN.search(lowered)
            and MinecraftConversationCommandParser.NUMBER_PATTERN.search(lowered)
        )

    def _clean_command(self, value: str) -> str:
        return " ".join(str(value or "").strip().strip("`\"'").split())
