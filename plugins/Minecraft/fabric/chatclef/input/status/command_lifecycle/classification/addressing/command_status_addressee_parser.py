#20260908_kpopmodder: Remove at most one normalized STATUS addressee by longest exact match.
from __future__ import annotations


class CommandStatusAddresseeParser:
    _PREFIXES = ("마크 ai ", "마인크래프트 ", "마크 ")

    def parse(self, normalized_text: str) -> tuple[str, str]:
        if type(normalized_text) is not str:
            raise TypeError("normalized command status text must be an exact str")
        for prefix in self._PREFIXES:
            if normalized_text.startswith(prefix):
                return prefix, normalized_text[len(prefix) :]
        return "", normalized_text


__all__ = ("CommandStatusAddresseeParser",)
