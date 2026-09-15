#20260915_kpopmodder: Parse explicit native coordinate variants while preserving legacy incomplete-XYZ refusals.
from __future__ import annotations

import re
import unicodedata

from .goto_grammar import MOVE_CONTEXT_RE, NONCOMMAND_RE, SIGNED_INTEGER, SEPARATOR
from .goto_parse_decision import GotoParseDecision as D
from .goto_parse_result import GotoParseResult


class KoreanGotoVariantParser:
    _DIMENSIONS = {"오버월드": "overworld", "지상": "overworld", "overworld": "overworld",
                   "네더": "nether", "지옥": "nether", "nether": "nether", "엔드": "end", "엔더": "end", "end": "end"}
    _MOVE = r"(?:으로|로)?\s*(?:가\s*(?:줘|주세요)|가자|가|이동해\s*(?:줘|주세요)|이동해|이동)[ .!]*"

    def parse(self, text: object) -> GotoParseResult | None:
        if not isinstance(text, str):
            return None
        value = unicodedata.normalize("NFKC", text).strip()
        if MOVE_CONTEXT_RE.search(value) is None:
            return None
        dimension_pattern = "|".join(self._DIMENSIONS)
        clue = re.search(r"(?:높이|평면\s*좌표)|(?:^|\s)(?:" + dimension_pattern + r")(?:\s|으로|로)|(?:^|\s)[yY]\s*[+-]?\d", value)
        xz_clue = re.search(r"(?:x|엑스).*?(?:z|제트)", value, re.I) and not re.search(r"(?:y|와이)", value, re.I)
        if not clue and not xz_clue:
            return None
        if NONCOMMAND_RE.search(value):
            return GotoParseResult(D.NONCOMMAND, reason_code="goto_noncommand", message="좌표 이동 명령이 아니어서 실행하지 않았어.")
        if any(unicodedata.category(c)[0] == "C" or (c.isspace() and c != " ") for c in text):
            return GotoParseResult(D.CLARIFY, reason_code="goto_control_character", message="좌표와 차원을 다시 알려줘.")
        move = re.fullmatch(r"(?P<body>.+?)\s*" + self._MOVE, value, re.I)
        if not move:
            return self._invalid()
        body = move["body"].strip()
        dimension = None
        prefix = re.match(r"^(" + dimension_pattern + r")(?:\s+|의\s*)", body, re.I)
        suffix = re.search(r"(?:\s+|^)(" + dimension_pattern + r")$", body, re.I)
        if body.lower() in self._DIMENSIONS:
            dimension, body = self._DIMENSIONS[body.lower()], ""
        elif prefix:
            dimension, body = self._DIMENSIONS[prefix[1].lower()], body[prefix.end():]
        elif suffix:
            dimension, body = self._DIMENSIONS[suffix[1].lower()], body[:suffix.start()]
        patterns = (
            rf"(?:x|엑스)\s*(?P<x>{SIGNED_INTEGER}){SEPARATOR}(?:z|제트)\s*(?P<z>{SIGNED_INTEGER})(?:\s*좌표)?",
            rf"(?P<x>{SIGNED_INTEGER}){SEPARATOR}(?P<z>{SIGNED_INTEGER})\s*평면\s*좌표",
            rf"(?:y|와이|높이)\s*(?P<y>{SIGNED_INTEGER})(?:\s*좌표)?",
        )
        numbers = None
        for pattern in patterns:
            match = re.fullmatch(pattern, body.strip(), re.I)
            if match:
                numbers = tuple(match.groupdict().values())
                break
        if dimension and numbers is None:
            body = re.sub(r"^좌표\s*|\s*좌표$", "", body).strip()
            if not body:
                numbers = ()
            elif re.fullmatch(rf"{SIGNED_INTEGER}(?:{SEPARATOR}{SIGNED_INTEGER}){{0,2}}", body):
                numbers = tuple(re.split(SEPARATOR, body))
        if numbers is None:
            return self._invalid()
        from .korean_goto_coordinate_parser import KoreanGotoCoordinateParser
        try:
            coordinates = tuple(KoreanGotoCoordinateParser._parse_integer(n, "coordinate") for n in numbers)
            return GotoParseResult(D.VALID_VARIANT, coordinates=coordinates, dimension=dimension)
        except (ValueError, TypeError):
            return self._invalid()

    @staticmethod
    def _invalid():
        return GotoParseResult(D.CLARIFY, reason_code="goto_invalid_variant", message="X/Z, 높이 Y 또는 차원을 정확히 지정해 줘.")
