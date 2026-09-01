#20260901_kpopmodder: Resolve StoreHome production-record fields without weakening duplicate or placeholder checks.
from __future__ import annotations

from collections.abc import Iterable

from ..diagnostic_record import ProductionDiagnosticRecord


def consistent_non_placeholder_field(
    records: tuple[ProductionDiagnosticRecord, ...],
    key: str,
    reason_label: str,
) -> tuple[str, str | None]:
    values: list[str] = []
    for record in records:
        value, error = required_field(record, key)
        if error is not None:
            return "", error
        if placeholder(value):
            return "", (
                f"P1_STORE_HOME_{reason_label}_PLACEHOLDER:{record.event_name}"
            )
        values.append(value)
    if len(set(values)) != 1:
        return "", f"P1_STORE_HOME_{reason_label}_MISMATCH"
    return values[0], None


def required_non_negative_integer(
    record: ProductionDiagnosticRecord,
    key: str,
) -> tuple[int, str | None]:
    value, error = required_field(record, key)
    if error is not None:
        return 0, error
    if not non_negative_canonical_integer(value):
        return 0, f"P1_STORE_HOME_FIELD_INTEGER_INVALID:{record.event_name}:{key}"
    return int(value), None


def required_field(
    record: ProductionDiagnosticRecord,
    key: str,
) -> tuple[str, str | None]:
    value, resolution = record.resolved_value(key)
    if value is None:
        suffix = (
            "MISSING"
            if resolution == "PRODUCTION_LOG_FIELD_MISSING"
            else "REPEATS_AMBIGUOUS"
        )
        return "", f"P1_STORE_HOME_FIELD_{suffix}:{record.event_name}:{key}"
    return value, None


def first_ambiguous_repeated_field(
    records: tuple[ProductionDiagnosticRecord, ...],
) -> tuple[str, str] | None:
    for record in records:
        for key in dict.fromkeys(key for key, _ in record.ordered_fields):
            if len(record.values(key)) < 2:
                continue
            value, _ = record.resolved_value(key)
            if value is None:
                return record.event_name, key
    return None


def record_identity_problem(record: ProductionDiagnosticRecord) -> str | None:
    event_name, error = required_field(record, "event")
    if error is not None:
        return error
    if event_name != record.event_name:
        return f"P1_STORE_HOME_RECORD_EVENT_NAME_INCONSISTENT:{record.event_name}"
    event_sequence, error = required_field(record, "eventSequence")
    if error is not None:
        return error
    if (
        not isinstance(record.event_sequence, int)
        or isinstance(record.event_sequence, bool)
        or not positive_canonical_integer(event_sequence)
        or int(event_sequence) != record.event_sequence
    ):
        return (
            "P1_STORE_HOME_RECORD_EVENT_SEQUENCE_INCONSISTENT:"
            f"{record.event_name}"
        )
    return None


def strictly_increasing(values: Iterable[int]) -> bool:
    previous: int | None = None
    for value in values:
        if previous is not None and value <= previous:
            return False
        previous = value
    return True


def positive_canonical_integer(value: str) -> bool:
    return non_negative_canonical_integer(value) and int(value) > 0


def non_negative_canonical_integer(value: str) -> bool:
    return value == "0" or (value.isascii() and value.isdecimal() and value[0] != "0")


def placeholder(value: str) -> bool:
    normalized = value.strip().casefold()
    return (
        not normalized
        or normalized in {"none", "null", "unavailable", "unverified"}
        or normalized.startswith("unavailable_")
    )
