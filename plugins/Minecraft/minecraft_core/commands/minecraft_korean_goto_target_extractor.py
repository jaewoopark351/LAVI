#20260725_kpopmodder: Added Korean coordinate target extraction for Minecraft goto commands.
from __future__ import annotations

import re


class MinecraftKoreanGotoTargetExtractor:
    NUMBER_PATTERN = re.compile(r"-?\d+")
    DIMENSION_ALIASES = (
        ("\uc624\ubc84\uc6d4\ub4dc", "overworld"),
        ("\uc624\ubc84 \uc6d4\ub4dc", "overworld"),
        ("\uc9c0\uc0c1\uc138\uacc4", "overworld"),
        ("\uc9c0\uc0c1", "overworld"),
        ("\ub124\ub354", "nether"),
        ("\uc9c0\uc625", "nether"),
        ("\uc5d4\ub4dc", "end"),
        ("\uc5d4\ub354", "end"),
    )

    def extract(self, text: object) -> str:
        lowered = str(text or "").strip().lower().replace(",", " ")
        numbers = self.NUMBER_PATTERN.findall(lowered)
        if len(numbers) < 3:
            return ""

        parts = list(numbers[:3])
        dimension = self._extract_dimension(lowered)
        if dimension:
            parts.append(dimension)
        return " ".join(parts)

    def _extract_dimension(self, lowered_text: str) -> str:
        for phrase, dimension in self.DIMENSION_ALIASES:
            if phrase in lowered_text:
                return dimension
        return ""
