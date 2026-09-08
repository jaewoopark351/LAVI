#20260908_kpopmodder: Validate only newly admitted raw STATUS-question shapes before normalization can erase separators.
from __future__ import annotations

import re
import unicodedata


class CommandStatusQuestionInputValidator:
    _MAXIMUM_LENGTH = 128
    _SOFT_PUNCTUATION = frozenset(",.!?，。！？")
    _TERMINAL_SOFT_PUNCTUATION = re.compile(r"[,.!?，。！？]+\Z")
    _FORBIDDEN_CHARACTERS = frozenset(
        "\"'`“”‘’«»‹›;；|｜&＆"
    )
    _RESTRICTED_PUNCTUATION = _SOFT_PUNCTUATION | _FORBIDDEN_CHARACTERS

    def accepts(self, value: object) -> bool:
        if type(value) is not str:
            return False
        trimmed = value.strip(" ")
        if not 1 <= len(trimmed) <= self._MAXIMUM_LENGTH:
            return False
        for character in trimmed:
            category = unicodedata.category(character)
            compatibility_form = unicodedata.normalize("NFKC", character)
            if (
                category in {"Cc", "Cf", "Zl", "Zp"}
                or (category == "Zs" and character != " ")
                or (character != " " and character.isspace())
                or (
                    category.startswith("P")
                    and character not in self._SOFT_PUNCTUATION
                )
                or character in self._FORBIDDEN_CHARACTERS
                or (
                    character not in self._RESTRICTED_PUNCTUATION
                    and any(
                        folded in self._RESTRICTED_PUNCTUATION
                        for folded in compatibility_form
                    )
                )
            ):
                return False
        terminal = self._TERMINAL_SOFT_PUNCTUATION.search(trimmed)
        body = trimmed[: terminal.start()] if terminal is not None else trimmed
        return bool(body.strip(" ")) and not any(
            character in self._SOFT_PUNCTUATION for character in body
        )


__all__ = ("CommandStatusQuestionInputValidator",)
