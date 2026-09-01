#20260831_kpopmodder: Parse only bounded unambiguous diagnostic records.
from __future__ import annotations

import re
from urllib.parse import unquote_to_bytes

from .diagnostic_parse_result import AutomaticDepositDiagnosticParseResult


def parse_bounded_diagnostic_event(
    line: object,
    *,
    max_utf8_bytes: int = 8192,
    max_fields: int = 64,
    max_key_chars: int = 96,
    max_value_chars: int = 512,
) -> AutomaticDepositDiagnosticParseResult:
    if not isinstance(line, str):
        return _failure("DIAGNOSTIC_LINE_NOT_TEXT")
    if max_utf8_bytes < 1 or max_fields < 1:
        return _failure("INVALID_PARSER_BOUND")
    if "\n" in line or "\r" in line:
        return _failure("DIAGNOSTIC_RECORD_NOT_ONE_COMPLETE_LINE")
    try:
        encoded = line.encode("utf-8", errors="strict")
    except UnicodeEncodeError:
        return _failure("DIAGNOSTIC_LINE_NOT_STRICT_UTF8")
    if len(encoded) > max_utf8_bytes:
        return _failure("DIAGNOSTIC_RECORD_EXCEEDS_BYTE_BOUND")

    markers = (
        "[LAVI ChatClefDiag]",
        "[LAVI ChatClefBoundary]",
        "[LAVI ChatClefLifecycle]",
    )
    occurrences = tuple(
        (marker, line.find(marker))
        for marker in markers
        if line.find(marker) >= 0
    )
    if len(occurrences) != 1:
        return _failure("DIAGNOSTIC_MARKER_MISSING_OR_AMBIGUOUS")
    marker, marker_offset = occurrences[0]
    if line.find(marker, marker_offset + len(marker)) >= 0:
        return _failure("DIAGNOSTIC_MARKER_MISSING_OR_AMBIGUOUS")

    payload = line[marker_offset + len(marker) :].strip()
    if not payload:
        return _failure("DIAGNOSTIC_PAYLOAD_EMPTY")
    tokens = tuple(payload.split())
    if len(tokens) > max_fields:
        return _failure("DIAGNOSTIC_FIELD_COUNT_EXCEEDS_BOUND")

    fields: list[tuple[str, str]] = []
    seen_keys: set[str] = set()
    for token in tokens:
        if token.count("=") != 1:
            return _failure("DIAGNOSTIC_TOKEN_AMBIGUOUS")
        key, value = token.split("=", 1)
        if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_.-]*", key):
            return _failure("DIAGNOSTIC_KEY_INVALID")
        if len(key) > max_key_chars or len(value) > max_value_chars:
            return _failure("DIAGNOSTIC_FIELD_EXCEEDS_BOUND")
        if not value:
            return _failure("DIAGNOSTIC_VALUE_EMPTY")
        if any(ord(character) < 0x20 or ord(character) == 0x7F for character in value):
            return _failure("DIAGNOSTIC_VALUE_HAS_CONTROL_CHARACTER")
        if key in seen_keys:
            return _failure("DIAGNOSTIC_DUPLICATE_KEY")
        seen_keys.add(key)
        decoded_value = _decode_percent_value(value)
        if decoded_value is None:
            return _failure("DIAGNOSTIC_VALUE_ENCODING_INVALID")
        fields.append((key, decoded_value))

    parsed = dict(fields)
    if parsed.get("diagnosticCaptureStatus") not in (
        None,
        "complete",
        "complete_no_active_candidate",
    ):
        return _failure("DIAGNOSTIC_CAPTURE_NOT_COMPLETE")
    if parsed.get("boundedPayloadUnavailable") == "true":
        return _failure("DIAGNOSTIC_BOUNDED_PAYLOAD_UNAVAILABLE")
    return AutomaticDepositDiagnosticParseResult(
        ok=True,
        reason="DIAGNOSTIC_RECORD_PARSED",
        marker=marker,
        fields=tuple(fields),
    )


def _failure(reason: str) -> AutomaticDepositDiagnosticParseResult:
    return AutomaticDepositDiagnosticParseResult(ok=False, reason=reason)


def _decode_percent_value(value: str) -> str | None:
    if value == "~EMPTY~":
        return ""
    for match in re.finditer("%", value):
        if not re.fullmatch(r"[0-9A-F]{2}", value[match.start() + 1 : match.start() + 3]):
            return None
    try:
        decoded = unquote_to_bytes(value).decode("utf-8", errors="strict")
    except UnicodeDecodeError:
        return None
    if any(ord(character) < 0x20 or ord(character) == 0x7F for character in decoded):
        return None
    return decoded
