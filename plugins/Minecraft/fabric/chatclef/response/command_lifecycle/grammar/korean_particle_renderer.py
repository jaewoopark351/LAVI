#20260907_kpopmodder: Select Korean particles from the final Hangul syllable.
from __future__ import annotations


class KoreanParticleRenderer:
    def attach(self, value: object, consonant: str, vowel: str) -> str:
        text = str(value or "").strip()
        if not text:
            return text
        return f"{text}{consonant if self._has_batchim(text[-1]) else vowel}"

    def attach_directional(self, value: object) -> str:
        text = str(value or "").strip()
        if not text:
            return text
        return f"{text}{'으로' if self._uses_euro(text[-1]) else '로'}"

    @staticmethod
    def _uses_euro(character: str) -> bool:
        if character.isascii() and character.isdigit():
            return character in {"0", "3", "6"}
        code = ord(character)
        if not 0xAC00 <= code <= 0xD7A3:
            return False
        jongseong = (code - 0xAC00) % 28
        return jongseong not in {0, 8}

    @staticmethod
    def _has_batchim(character: str) -> bool:
        if character.isascii() and character.isdigit():
            return character in {"0", "1", "3", "6", "7", "8"}
        code = ord(character)
        return 0xAC00 <= code <= 0xD7A3 and (code - 0xAC00) % 28 != 0


__all__ = ("KoreanParticleRenderer",)
