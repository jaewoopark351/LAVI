#20260725_kpopmodder: Added parser for goto-style Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_command_aliases import DIMENSION_ALIASES, GOTO_PREFIXES, GOTO_WORDS
from .minecraft_text_parse_helpers import (
    ITEM_TOKEN_PATTERN,
    NUMBER_PATTERN,
    build_payload,
    squash_spaces,
)


class MinecraftGotoCommandParser:
    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        target = self._target_text(text, lowered)
        if not target:
            return None
        payload = build_payload("goto", text)
        payload["target"] = target
        return payload

    def _target_text(self, text: str, lowered: str) -> str:
        for prefix in GOTO_PREFIXES:
            if lowered == prefix:
                return ""
            if lowered.startswith(prefix + " "):
                return self._normalize_goto_target(text[len(prefix) :].strip())

        if any(word in lowered for word in GOTO_WORDS):
            return self._extract_goto_target(text)
        return ""

    def _normalize_goto_target(self, text: str) -> str:
        cleaned = squash_spaces(text.replace(",", " "))
        parts = cleaned.split(" ")
        if not parts:
            return ""
        normalized_parts = []
        for part in parts:
            lowered = part.lower()
            normalized_parts.append(DIMENSION_ALIASES.get(lowered, lowered))
        return " ".join(normalized_parts)

    def _extract_goto_target(self, text: str) -> str:
        lowered = text.lower().replace(",", " ")
        numbers = NUMBER_PATTERN.findall(lowered)
        if len(numbers) > 3:
            return ""
        dimension = self._extract_dimension(lowered)
        parts = list(numbers)
        if dimension:
            parts.append(dimension)
        return " ".join(parts)

    def _extract_dimension(self, lowered_text: str) -> str:
        for token in ITEM_TOKEN_PATTERN.findall(lowered_text):
            normalized = DIMENSION_ALIASES.get(token.lower())
            if normalized:
                return normalized
        for alias, normalized in DIMENSION_ALIASES.items():
            if alias in lowered_text:
                return normalized
        return ""
