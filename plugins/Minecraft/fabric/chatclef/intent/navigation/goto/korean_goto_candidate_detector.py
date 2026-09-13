#20260913_kpopmodder: Detect coordinate movement context without authorizing or parsing XYZ.
from __future__ import annotations

import unicodedata

from .goto_grammar import AXIS_CLUE_RE, HANGUL_NUMBER_RE, MOVE_CONTEXT_RE, NUMERIC_CLUE_RE


class KoreanGotoCandidateDetector:
    def is_candidate(self, text: object) -> bool:
        if not isinstance(text, str):
            return False
        normalized = unicodedata.normalize("NFKC", text).lower()
        if MOVE_CONTEXT_RE.search(normalized) is None:
            return False
        if "좌표" in normalized or AXIS_CLUE_RE.search(normalized) is not None:
            return True
        numeric_clues = NUMERIC_CLUE_RE.findall(normalized)
        if len(numeric_clues) >= 2 or len(HANGUL_NUMBER_RE.findall(normalized)) >= 2:
            return True
        # One leading number is still an incomplete coordinate request, not an item count.
        return bool(numeric_clues) and normalized.lstrip(" \"'(").startswith(
            tuple("0123456789+-~^")
        )
