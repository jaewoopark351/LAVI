#20260905_kpopmodder: Classify only guarded, whole-utterance Korean STOP phrases.
from __future__ import annotations

import re
import unicodedata
from types import MappingProxyType

from .stop_input_decision import StopInputDecision
from .stop_input_decision_kind import StopInputDecisionKind


class KoreanStopInputClassifier:
    _PHRASES = MappingProxyType(
        {
            "멈춰": "stop_bare",
            "멈춰줘": "stop_request",
            "중지": "stop_halt",
            "정지": "stop_pause",
            "스톱": "stop_loanword",
            "그만": "stop_enough",
            "마크 ai 멈춰": "stop_mark_ai",
            "마크 ai 멈춰줘": "stop_mark_ai_request",
            "마인크래프트 ai 멈춰": "stop_minecraft_ai",
            "마인크래프트 ai 멈춰줘": "stop_minecraft_ai_request",
        }
    )
    _TERMINAL_SUFFIXES = frozenset({".", "。", "!", "！"})
    _FORBIDDEN_GUARD = frozenset(
        {"?", "？", '"', "'", "“", "”", "‘", "’", "「", "」", "『", "』", ";", "；"}
    )
    _STOP_TOKEN = re.compile(r"(?<![가-힣A-Za-z0-9_])(멈춰(?:줘)?|중지|정지|스톱|그만)(?![가-힣A-Za-z0-9_])")

    def classify(self, raw_text: object) -> StopInputDecision:
        if type(raw_text) is not str:
            return self._unrelated("raw_text_not_exact_str")
        trimmed = self._trim_exact_white_space(raw_text)
        if not trimmed:
            return self._unrelated("empty")
        if self._contains_forbidden_character(trimmed):
            return self._guarded_if_stop_like(trimmed, "unsafe_raw_character")

        body, suffix, suffix_error = self._detach_terminal_suffix(trimmed)
        if suffix_error:
            return self._guarded_if_stop_like(trimmed, suffix_error)
        if self._contains_disallowed_internal_character(body):
            return self._guarded_if_stop_like(body, "unsafe_internal_character")

        normalized = unicodedata.normalize("NFKC", body)
        normalized = re.sub(r" +", " ", normalized).strip(" ")
        normalized = re.sub(r"[A-Z]", lambda match: match.group(0).lower(), normalized)
        rule_id = self._PHRASES.get(normalized)
        if rule_id is None:
            return self._guarded_if_stop_like(normalized, "not_exact_whole_utterance")
        return StopInputDecision(
            kind=StopInputDecisionKind.VALID_EXACT_STOP,
            phrase_rule_id=rule_id,
            normalized_phrase=normalized,
            terminal_suffix=suffix,
            reason="exact_stop_phrase",
        )

    def _trim_exact_white_space(self, value: str) -> str:
        start = 0
        end = len(value)
        while start < end and self._is_trim_code_point(ord(value[start])):
            start += 1
        while end > start and self._is_trim_code_point(ord(value[end - 1])):
            end -= 1
        return value[start:end]

    def _is_trim_code_point(self, code_point: int) -> bool:
        return (
            0x0009 <= code_point <= 0x000D
            or code_point in {0x0020, 0x0085, 0x00A0, 0x1680, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000}
            or 0x2000 <= code_point <= 0x200A
        )

    def _contains_forbidden_character(self, value: str) -> bool:
        return any(character in self._FORBIDDEN_GUARD for character in value)

    def _contains_disallowed_internal_character(self, value: str) -> bool:
        for character in value:
            if character in self._TERMINAL_SUFFIXES:
                return True
            if character == " ":
                continue
            category = unicodedata.category(character)
            if category in {"Cc", "Cf", "Zl", "Zp"}:
                return True
            if character.isspace():
                return True
            if category.startswith("P"):
                return True
        return False

    def _detach_terminal_suffix(self, value: str) -> tuple[str, str, str]:
        if value[-1] not in self._TERMINAL_SUFFIXES:
            if any(character in self._TERMINAL_SUFFIXES for character in value):
                return value, "", "internal_terminal_punctuation"
            return value, "", ""
        suffix = value[-1]
        body = value[:-1]
        if not body or body[-1].isspace():
            return value, "", "detached_terminal_punctuation"
        if any(character in self._TERMINAL_SUFFIXES for character in body):
            return value, "", "repeated_terminal_punctuation"
        return body, suffix, ""

    def _guarded_if_stop_like(self, value: str, reason: str) -> StopInputDecision:
        candidate = unicodedata.normalize("NFKC", value).lower()
        if self._STOP_TOKEN.search(candidate):
            return StopInputDecision(
                kind=StopInputDecisionKind.GUARDED_STOP_LIKE_REJECTION,
                reason=reason,
            )
        return self._unrelated(reason)

    def _unrelated(self, reason: str) -> StopInputDecision:
        return StopInputDecision(
            kind=StopInputDecisionKind.UNRELATED,
            reason=reason,
        )


__all__ = ("KoreanStopInputClassifier",)
