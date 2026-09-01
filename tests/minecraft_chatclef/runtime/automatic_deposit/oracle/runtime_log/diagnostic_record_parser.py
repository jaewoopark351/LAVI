#20260901_kpopmodder: Parse only complete bounded Java diagnostic envelopes from one production log line.
from __future__ import annotations

import re

from .diagnostic_record import ProductionDiagnosticRecord, resolve_repeated_values
from .diagnostic_record_parse_result import ProductionDiagnosticRecordParseResult

MAX_JAVA_RECORD_UTF8_BYTES = 8192
MAX_OUTER_LOG_LINE_UTF8_BYTES = 16384
MAX_RECORD_FIELDS = 256
MAX_KEY_ASCII_CHARS = 96
MAX_ENCODED_VALUE_CHARS = 2048

BOUNDED_EVENT_ENVELOPE = "BOUNDED_EVENT"
AUTO_DEPOSIT_ALL_PHASE_ENVELOPE = "AUTO_DEPOSIT_ALL_PHASE"

_BOUNDARY_MARKER = "[LAVI ChatClefBoundary]"
_DIAGNOSTIC_MARKER = "[LAVI ChatClefDiag]"
_JAVA_RECORD_PREFIX = "ALTO CLEF: "
_MARKERS = (_BOUNDARY_MARKER, _DIAGNOSTIC_MARKER)
_KEY_PATTERN = re.compile(r"[A-Za-z][A-Za-z0-9_.-]*\Z", re.ASCII)
_SEQUENCE_PATTERN = re.compile(r"[1-9][0-9]{0,18}\Z", re.ASCII)
_LONG_MAX = 9_223_372_036_854_775_807
_COMPLETE_CAPTURE_STATUSES = frozenset(
    {"complete", "complete_no_active_candidate"}
)
_BOUNDARY_REQUIRED_KEYS = (
    "traceId",
    "clientTickId",
    "eventSequence",
    "taskInstanceId",
    "taskRunId",
    "parentTaskRunId",
    "threadName",
    "level",
    "event",
    "reason",
    "taskClass",
    "diagnosticCaptureStatus",
)
_AUTO_DEPOSIT_ALL_REQUIRED_KEYS = (
    "traceId",
    "clientTickId",
    "eventSequence",
    "taskInstanceId",
    "taskRunId",
    "parentTaskRunId",
    "threadName",
    "eventType",
    "phase",
    "reason",
    "taskClass",
)


def parse_production_diagnostic_record(
    line: object,
    *,
    max_record_utf8_bytes: int = MAX_JAVA_RECORD_UTF8_BYTES,
    max_outer_line_utf8_bytes: int = MAX_OUTER_LOG_LINE_UTF8_BYTES,
    max_fields: int = MAX_RECORD_FIELDS,
    max_key_chars: int = MAX_KEY_ASCII_CHARS,
    max_encoded_value_chars: int = MAX_ENCODED_VALUE_CHARS,
) -> ProductionDiagnosticRecordParseResult:
    if not isinstance(line, str):
        return _failure("PRODUCTION_LOG_LINE_NOT_TEXT")
    requested_bounds = (
        (max_record_utf8_bytes, MAX_JAVA_RECORD_UTF8_BYTES),
        (max_outer_line_utf8_bytes, MAX_OUTER_LOG_LINE_UTF8_BYTES),
        (max_fields, MAX_RECORD_FIELDS),
        (max_key_chars, MAX_KEY_ASCII_CHARS),
        (max_encoded_value_chars, MAX_ENCODED_VALUE_CHARS),
    )
    if any(requested < 1 or requested > hard_max for requested, hard_max in requested_bounds):
        return _failure("PRODUCTION_LOG_PARSER_BOUND_INVALID")
    if "\n" in line or "\r" in line:
        return _failure("PRODUCTION_LOG_RECORD_NOT_ONE_COMPLETE_LINE")
    if any(ord(character) < 0x20 or ord(character) == 0x7F for character in line):
        return _failure("PRODUCTION_LOG_RECORD_HAS_CONTROL_CHARACTER")
    try:
        encoded_line = line.encode("utf-8", errors="strict")
    except UnicodeEncodeError:
        return _failure("PRODUCTION_LOG_LINE_NOT_STRICT_UTF8")
    if len(encoded_line) > max_outer_line_utf8_bytes:
        return _failure("PRODUCTION_LOG_OUTER_LINE_EXCEEDS_BYTE_BOUND")
    if line.count(_JAVA_RECORD_PREFIX) > 1:
        return _failure("PRODUCTION_LOG_JAVA_RECORD_PREFIX_AMBIGUOUS")

    marker_occurrences = tuple(
        (marker, offset)
        for marker in _MARKERS
        for offset in _all_offsets(line, marker)
    )
    if len(marker_occurrences) != 1:
        return _failure("PRODUCTION_LOG_MARKER_MISSING_OR_AMBIGUOUS")
    marker, marker_offset = marker_occurrences[0]
    java_record_start = marker_offset - len(_JAVA_RECORD_PREFIX)
    if (
        java_record_start < 0
        or line[java_record_start:marker_offset] != _JAVA_RECORD_PREFIX
    ):
        return _failure("PRODUCTION_LOG_JAVA_RECORD_PREFIX_INVALID")
    java_record = line[java_record_start:]
    if len(java_record.encode("utf-8", errors="strict")) > max_record_utf8_bytes:
        return _failure("PRODUCTION_LOG_RECORD_EXCEEDS_BYTE_BOUND")
    payload_start = marker_offset + len(marker)
    if payload_start >= len(line) or line[payload_start] != " ":
        return _failure("PRODUCTION_LOG_MARKER_DELIMITER_INVALID")
    payload = line[payload_start + 1 :]
    if not payload:
        return _failure("PRODUCTION_LOG_PAYLOAD_EMPTY")
    if payload.startswith(" ") or payload.endswith(" ") or "  " in payload:
        return _failure("PRODUCTION_LOG_FIELD_DELIMITER_INVALID")

    tokens = tuple(payload.split(" "))
    if len(tokens) > max_fields:
        return _failure("PRODUCTION_LOG_FIELD_COUNT_EXCEEDS_BOUND")

    ordered_fields: list[tuple[str, str]] = []
    for token in tokens:
        if token.count("=") != 1:
            return _failure("PRODUCTION_LOG_TOKEN_AMBIGUOUS")
        key, encoded_value = token.split("=", 1)
        if _KEY_PATTERN.fullmatch(key) is None:
            return _failure("PRODUCTION_LOG_KEY_INVALID")
        if len(key) > max_key_chars:
            return _failure("PRODUCTION_LOG_KEY_EXCEEDS_BOUND")
        if not encoded_value:
            return _failure("PRODUCTION_LOG_VALUE_EMPTY")
        if len(encoded_value) > max_encoded_value_chars:
            return _failure("PRODUCTION_LOG_VALUE_EXCEEDS_BOUND")
        decoded_value = _decode_java_diagnostic_value(encoded_value)
        if decoded_value is None:
            return _failure("PRODUCTION_LOG_VALUE_ENCODING_INVALID")
        ordered_fields.append((key, decoded_value))

    fields = tuple(ordered_fields)
    event_values = _values(fields, "event")
    event_type_values = _values(fields, "eventType")
    if event_values and event_type_values:
        return _failure("PRODUCTION_LOG_ENVELOPE_AMBIGUOUS")
    if event_values:
        return _parse_bounded_event(marker, fields)
    if event_type_values:
        return _parse_auto_deposit_all_event(marker, fields)
    return _failure("PRODUCTION_LOG_ENVELOPE_UNSUPPORTED")


def _parse_bounded_event(
    marker: str,
    fields: tuple[tuple[str, str], ...],
) -> ProductionDiagnosticRecordParseResult:
    if marker != _BOUNDARY_MARKER:
        return _failure("PRODUCTION_LOG_BOUNDARY_MARKER_INVALID")
    missing_or_ambiguous = _required_field_problem(fields, _BOUNDARY_REQUIRED_KEYS)
    if missing_or_ambiguous is not None:
        return _failure(missing_or_ambiguous)

    event_name, _ = resolve_repeated_values(_values(fields, "event"))
    level, _ = resolve_repeated_values(_values(fields, "level"))
    capture_status, _ = resolve_repeated_values(
        _values(fields, "diagnosticCaptureStatus")
    )
    if level != "BOUNDARY":
        return _failure("PRODUCTION_LOG_BOUNDARY_LEVEL_INVALID")
    if capture_status not in _COMPLETE_CAPTURE_STATUSES:
        return _failure("PRODUCTION_LOG_CAPTURE_NOT_COMPLETE")
    bounded_payload_problem = _bounded_payload_problem(fields)
    if bounded_payload_problem is not None:
        return _failure(bounded_payload_problem)

    sequence = _event_sequence(fields)
    if sequence is None:
        return _failure("PRODUCTION_LOG_EVENT_SEQUENCE_INVALID")
    return _success(
        ProductionDiagnosticRecord(
            marker=marker,
            envelope_kind=BOUNDED_EVENT_ENVELOPE,
            event_name=event_name or "",
            event_sequence=sequence,
            ordered_fields=fields,
        )
    )


def _parse_auto_deposit_all_event(
    marker: str,
    fields: tuple[tuple[str, str], ...],
) -> ProductionDiagnosticRecordParseResult:
    if marker != _DIAGNOSTIC_MARKER:
        return _failure("PRODUCTION_LOG_AUTO_DEPOSIT_MARKER_INVALID")
    missing_or_ambiguous = _required_field_problem(
        fields, _AUTO_DEPOSIT_ALL_REQUIRED_KEYS
    )
    if missing_or_ambiguous is not None:
        return _failure(missing_or_ambiguous)

    event_type, _ = resolve_repeated_values(_values(fields, "eventType"))
    phase, _ = resolve_repeated_values(_values(fields, "phase"))
    if event_type != "AUTO_DEPOSIT_ALL":
        return _failure("PRODUCTION_LOG_EVENT_TYPE_UNSUPPORTED")
    if phase is None or _is_unavailable(phase):
        return _failure("PRODUCTION_LOG_AUTO_DEPOSIT_PHASE_INVALID")

    capture_values = _values(fields, "diagnosticCaptureStatus")
    if capture_values:
        capture_status, capture_reason = resolve_repeated_values(capture_values)
        if capture_reason == "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS":
            return _failure("PRODUCTION_LOG_REQUIRED_FIELD_AMBIGUOUS")
        if capture_status not in _COMPLETE_CAPTURE_STATUSES:
            return _failure("PRODUCTION_LOG_CAPTURE_NOT_COMPLETE")
    bounded_payload_problem = _bounded_payload_problem(fields)
    if bounded_payload_problem is not None:
        return _failure(bounded_payload_problem)
    sequence = _event_sequence(fields)
    if sequence is None:
        return _failure("PRODUCTION_LOG_EVENT_SEQUENCE_INVALID")
    return _success(
        ProductionDiagnosticRecord(
            marker=marker,
            envelope_kind=AUTO_DEPOSIT_ALL_PHASE_ENVELOPE,
            event_name=phase,
            event_sequence=sequence,
            ordered_fields=fields,
        )
    )


def _required_field_problem(
    fields: tuple[tuple[str, str], ...],
    required_keys: tuple[str, ...],
) -> str | None:
    for key in required_keys:
        value, reason = resolve_repeated_values(_values(fields, key))
        if reason == "PRODUCTION_LOG_FIELD_MISSING":
            return "PRODUCTION_LOG_REQUIRED_FIELD_MISSING"
        if (
            reason == "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS"
            or value is None
            or value == ""
        ):
            return "PRODUCTION_LOG_REQUIRED_FIELD_AMBIGUOUS"
    return None


def _event_sequence(fields: tuple[tuple[str, str], ...]) -> int | None:
    raw_sequence, reason = resolve_repeated_values(_values(fields, "eventSequence"))
    if reason == "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS" or raw_sequence is None:
        return None
    if _SEQUENCE_PATTERN.fullmatch(raw_sequence) is None:
        return None
    sequence = int(raw_sequence)
    return sequence if sequence <= _LONG_MAX else None


def _bounded_payload_problem(
    fields: tuple[tuple[str, str], ...],
) -> str | None:
    values = _values(fields, "boundedPayloadUnavailable")
    if not values:
        return None
    unavailable, unavailable_reason = resolve_repeated_values(values)
    if unavailable_reason == "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS":
        return "PRODUCTION_LOG_BOUNDED_PAYLOAD_FLAG_AMBIGUOUS"
    if unavailable not in {"true", "false"}:
        return "PRODUCTION_LOG_BOUNDED_PAYLOAD_FLAG_INVALID"
    if unavailable == "true":
        return "PRODUCTION_LOG_BOUNDED_PAYLOAD_UNAVAILABLE"
    return None


def _values(
    fields: tuple[tuple[str, str], ...],
    key: str,
) -> tuple[str, ...]:
    return tuple(value for field_key, value in fields if field_key == key)


def _decode_java_diagnostic_value(encoded_value: str) -> str | None:
    if encoded_value == "~EMPTY~":
        return ""
    decoded_bytes = bytearray()
    offset = 0
    while offset < len(encoded_value):
        character = encoded_value[offset]
        if character == "%":
            digits = encoded_value[offset + 1 : offset + 3]
            if len(digits) != 2 or re.fullmatch(r"[0-9A-F]{2}", digits) is None:
                return None
            decoded_bytes.append(int(digits, 16))
            offset += 3
            continue
        code_point = ord(character)
        if code_point < 0x21 or code_point > 0x7E or character in "=~":
            return None
        decoded_bytes.append(code_point)
        offset += 1
    try:
        decoded = bytes(decoded_bytes).decode("utf-8", errors="strict")
    except UnicodeDecodeError:
        return None
    if any(ord(character) < 0x20 or ord(character) == 0x7F for character in decoded):
        return None
    return decoded


def _is_unavailable(value: str) -> bool:
    return value == "UNAVAILABLE" or value.startswith("UNAVAILABLE_")


def _all_offsets(value: str, needle: str) -> tuple[int, ...]:
    offsets: list[int] = []
    start = 0
    while True:
        offset = value.find(needle, start)
        if offset < 0:
            return tuple(offsets)
        offsets.append(offset)
        start = offset + len(needle)


def _success(
    record: ProductionDiagnosticRecord,
) -> ProductionDiagnosticRecordParseResult:
    return ProductionDiagnosticRecordParseResult(
        ok=True,
        reason="PRODUCTION_LOG_RECORD_PARSED",
        record=record,
    )


def _failure(reason: str) -> ProductionDiagnosticRecordParseResult:
    return ProductionDiagnosticRecordParseResult(ok=False, reason=reason)
