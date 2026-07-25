#20260725_kpopmodder: Added Korean count parsing for Minecraft natural commands.
from __future__ import annotations

import re


class MinecraftKoreanCountParser:
    NUMBER_PATTERN = re.compile(r"\d+")
    COUNT_WORDS = (
        ("\uc5f4", 10),
        ("\uc544\ud649", 9),
        ("\uc5ec\ub35f", 8),
        ("\uc77c\uacf1", 7),
        ("\uc5ec\uc12f", 6),
        ("\ub2e4\uc12f", 5),
        ("\ub124", 4),
        ("\ub137", 4),
        ("\uc138", 3),
        ("\uc14b", 3),
        ("\ub450", 2),
        ("\ub458", 2),
        ("\ud558\ub098", 1),
        ("\ud55c\uac1c", 1),
        ("\ud55c \uac1c", 1),
        ("\ud55c", 1),
    )

    def parse(self, text: object, default: int = 1) -> int:
        raw_text = str(text or "")
        number_match = self.NUMBER_PATTERN.search(raw_text)
        if number_match:
            return max(int(number_match.group(0)), 1)

        for word, value in self.COUNT_WORDS:
            if word in raw_text:
                return value
        return default
