#20260815_kpopmodder: Centralize Korean acquisition and mining verb matching for ChatClef.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)


class KoreanAcquisitionVerbMatcher:
    _ACQUIRE_PATTERNS = (
        r"가져\s*와\s*줘",
        r"가져\s*와",
        r"가져\s*다\s*줘",
        r"구해\s*줘",
        r"구해\s*주세요",
        r"구해",
        r"얻어\s*줘",
        r"얻어\s*주세요",
        r"얻어",
    )
    _CRAFT_PATTERNS = (
        r"만들어\s*줘",
        r"만들어\s*주세요",
        r"만들어",
        r"제작해\s*줘",
        r"제작해\s*주세요",
        r"제작해",
        r"제작",
    )
    _MINING_PATTERNS = (
        r"캐\s*와\s*서\s*가져\s*와\s*줘",
        r"캐\s*와\s*서\s*가져\s*와",
        r"채굴\s*해서\s*가져\s*와\s*줘",
        r"채굴\s*해서\s*가져\s*와",
        r"채굴\s*해\s*주세요",
        r"채굴\s*해\s*줘",
        r"채굴\s*해",
        r"캐\s*와\s*줘",
        r"캐\s*와",
        r"캐\s*오기",
        r"캐\s*주세요",
        r"캐\s*줘",
    )

    def __init__(self, normalizer: KoreanTextNormalizer | None = None):
        self._normalizer = normalizer or KoreanTextNormalizer()
        self._verb_re = re.compile(
            self._alternation(
                self._ACQUIRE_PATTERNS
                + self._CRAFT_PATTERNS
                + self._MINING_PATTERNS
            )
        )
        self._mining_re = re.compile(self._alternation(self._MINING_PATTERNS))
        self._craft_re = re.compile(self._alternation(self._CRAFT_PATTERNS))

    def matches(self, text: object) -> bool:
        return self._verb_re.search(self._normalize(text)) is not None

    def classify(self, text: object) -> str:
        normalized = self._normalize(text)
        if self._mining_re.search(normalized):
            return "mining"
        if self._craft_re.search(normalized):
            return "craft"
        if self._verb_re.search(normalized):
            return "acquire"
        return ""

    def strip(self, text: object) -> str:
        stripped = self._verb_re.sub(" ", self._normalize(text))
        return re.sub(r"\s+", " ", stripped).strip()

    def _normalize(self, text: object) -> str:
        return self._normalizer.normalize(text)

    def _alternation(self, patterns: tuple[str, ...]) -> str:
        ordered = sorted(patterns, key=len, reverse=True)
        return rf"(?:{'|'.join(ordered)})(?=$|[^가-힣A-Za-z0-9_])"
