#20260803_kpopmodder: Added quantity parsing separate from Korean intent rules.
from __future__ import annotations

import re


class KoreanQuantityParser:
    _DIGIT_RE = re.compile(r"(?<![A-Za-z0-9_])([+-]?\d+)\s*(개|만큼)")
    _KOREAN_NUMBERS = {
        "하나": 1,
        "한": 1,
        "둘": 2,
        "두": 2,
        "셋": 3,
        "세": 3,
        "넷": 4,
        "네": 4,
    }

    def parse(self, text: str, default: int = 1) -> int:
        parsed = self.parse_optional(text)
        if parsed is None:
            return default
        return parsed

    def parse_optional(self, text: str) -> int | None:
        match = self._DIGIT_RE.search(text)
        if match is not None:
            return int(match.group(1))
        korean_match = self._korean_match(text)
        if korean_match is None:
            return None
        return self._KOREAN_NUMBERS[korean_match.group(1)]

    def strip_quantity(self, text: str) -> str:
        stripped = self._DIGIT_RE.sub(" ", text)
        korean_match = self._korean_match(stripped)
        if korean_match is not None:
            start, end = korean_match.span()
            stripped = f"{stripped[:start]} {stripped[end:]}"
        return re.sub(r"\s+", " ", stripped).strip()

    def _korean_match(self, text: str) -> re.Match[str] | None:
        words = "|".join(sorted(self._KOREAN_NUMBERS, key=len, reverse=True))
        pattern = re.compile(
            rf"(?<![가-힣])({words})(?:\s*(?:개|만큼))?(?![가-힣])"
        )
        return pattern.search(text)
