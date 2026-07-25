#20260725_kpopmodder: Added Korean equip intent detection with STT typo handling.
from __future__ import annotations

import re


class MinecraftKoreanEquipSignalDetector:
    HOLD_PATTERNS = (
        re.compile(r"(?<!\ub9cc)\ub4e4\uc5b4"),
        re.compile(r"(?<!\ub9cc)\ub4e4\uace0"),
    )
    EQUIP_WORDS = (
        "\uc7a5\ucc29",
        "\ucc29\uc6a9",
        "\uc950\uc5b4",
        "\ub07c\uc6cc",
    )
    EQUIP_STT_PATTERNS = (
        re.compile(r"\uc7a5\ucc45\s*\ud574"),
        re.compile(r"\uc7a5\uc791\s*\ud574"),
        re.compile(r"\uc7a5\ucc29\s*\ud574[\uc870\uc8e0]"),
    )

    def has_signal(self, text: str) -> bool:
        if any(word in text for word in self.EQUIP_WORDS):
            return True
        if any(pattern.search(text) for pattern in self.HOLD_PATTERNS):
            return True
        return any(pattern.search(text) for pattern in self.EQUIP_STT_PATTERNS)
