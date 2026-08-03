#20260803_kpopmodder: Added Korean text normalization for deterministic parsing.
from __future__ import annotations

import re
import unicodedata


class KoreanTextNormalizer:
    _WHITESPACE_RE = re.compile(r"\s+")
    _SOFT_PUNCTUATION_RE = re.compile(r"[,.!?，。！？]+")

    def normalize(self, value: object, lowercase_english: bool = True) -> str:
        text = unicodedata.normalize("NFKC", str(value or ""))
        text = self._SOFT_PUNCTUATION_RE.sub(" ", text)
        text = self._WHITESPACE_RE.sub(" ", text).strip()
        if lowercase_english:
            text = re.sub(
                r"[A-Za-z]+",
                lambda match: match.group(0).lower(),
                text,
            )
        return text
