#20260905_kpopmodder: Project only whether normalized input contains Hangul.
from __future__ import annotations

import unicodedata


class MinecraftKoreanLanguageStatusProjector:
    @staticmethod
    def contains_hangul(value: object) -> bool:
        if type(value) is not str:
            return False
        normalized = unicodedata.normalize("NFKC", value)
        return any(
            "\u1100" <= character <= "\u11ff"
            or "\u3130" <= character <= "\u318f"
            or "\ua960" <= character <= "\ua97f"
            or "\uac00" <= character <= "\ud7a3"
            or "\ud7b0" <= character <= "\ud7ff"
            for character in normalized
        )


__all__ = ("MinecraftKoreanLanguageStatusProjector",)
