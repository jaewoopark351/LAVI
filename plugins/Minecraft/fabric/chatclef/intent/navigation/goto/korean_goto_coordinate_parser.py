#20260913_kpopmodder: Parse complete raw Korean absolute XYZ commands before lossy normalization.
from __future__ import annotations

import unicodedata

from plugins.Minecraft.fabric.chatclef.intent.chatclef_numeric_constraints import (
    ChatClefNumericConstraints,
)

from .goto_grammar import COORDINATE_COMMAND_PATTERNS, NONCOMMAND_RE
from .goto_parse_decision import GotoParseDecision
from .goto_parse_result import GotoParseResult
from .korean_goto_candidate_detector import KoreanGotoCandidateDetector


class KoreanGotoCoordinateParser:
    def __init__(self, candidate_detector: KoreanGotoCandidateDetector | None = None):
        self._candidate_detector = candidate_detector or KoreanGotoCandidateDetector()

    def parse(self, text: object) -> GotoParseResult:
        #20260915_kpopmodder: Additional native forms use the same raw-input guard and binding path.
        from .korean_goto_variant_parser import KoreanGotoVariantParser
        # Exact existing XYZ has priority over the additional Y/XZ clue detector.
        canonical_xyz = isinstance(text, str) and any(pattern.fullmatch(unicodedata.normalize("NFKC", text)) for pattern in COORDINATE_COMMAND_PATTERNS)
        variant = None if canonical_xyz else KoreanGotoVariantParser().parse(text)
        if variant is not None:
            return variant
        if not self._candidate_detector.is_candidate(text):
            return GotoParseResult(GotoParseDecision.NOT_CANDIDATE)
        if any(unicodedata.category(character) in {"Cc", "Cf", "Cs"} for character in text):
            return self._clarify("goto_control_character")
        if any(character.isspace() and character != " " for character in text):
            return self._clarify("goto_unsupported_whitespace")
        normalized = unicodedata.normalize("NFKC", text)
        if NONCOMMAND_RE.search(normalized) is not None:
            return GotoParseResult(
                GotoParseDecision.NONCOMMAND,
                reason_code="goto_noncommand",
                message="좌표 이동 명령이 아니어서 실행하지 않았어.",
            )
        for pattern in COORDINATE_COMMAND_PATTERNS:
            match = pattern.fullmatch(normalized)
            if match is None:
                continue
            try:
                xyz = tuple(self._parse_integer(match.group(axis), axis) for axis in ("x", "y", "z"))
            except (TypeError, ValueError):
                return self._clarify("goto_coordinate_out_of_range")
            return GotoParseResult(GotoParseDecision.VALID_XYZ, xyz=xyz)
        return self._clarify("goto_invalid_coordinates")

    @staticmethod
    def _parse_integer(raw: str, axis: str) -> int:
        if raw.startswith("마이너스"):
            raw = "-" + raw[len("마이너스"):].lstrip(" ")
        elif raw.startswith("플러스"):
            raw = "+" + raw[len("플러스"):].lstrip(" ")
        digits = raw.lstrip("+-").lstrip("0") or "0"
        if len(digits) > 10:
            raise ValueError("coordinate_out_of_java_int_range")
        sign = -1 if raw.startswith("-") else 1
        value = ChatClefNumericConstraints.required_exact_int(sign * int(digits), axis)
        return ChatClefNumericConstraints.java_int(value, axis)

    @staticmethod
    def _clarify(reason_code: str) -> GotoParseResult:
        return GotoParseResult(
            GotoParseDecision.CLARIFY,
            reason_code=reason_code,
            message="X, Y, Z 좌표 세 개를 다시 알려줘.",
        )
