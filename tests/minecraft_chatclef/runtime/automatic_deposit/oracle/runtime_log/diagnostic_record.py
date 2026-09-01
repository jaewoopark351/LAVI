#20260901_kpopmodder: Preserve one parsed production diagnostic record and resolve repeated fields conservatively.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ProductionDiagnosticRecord:
    marker: str
    envelope_kind: str
    event_name: str
    event_sequence: int
    ordered_fields: tuple[tuple[str, str], ...]

    def values(self, key: str) -> tuple[str, ...]:
        return tuple(value for field_key, value in self.ordered_fields if field_key == key)

    def resolved_value(
        self,
        key: str,
        *,
        none_is_placeholder: bool = False,
    ) -> tuple[str | None, str]:
        return resolve_repeated_values(
            self.values(key),
            none_is_placeholder=none_is_placeholder,
        )


def resolve_repeated_values(
    values: tuple[str, ...],
    *,
    none_is_placeholder: bool = False,
) -> tuple[str | None, str]:
    if not values:
        return None, "PRODUCTION_LOG_FIELD_MISSING"
    if len(values) == 1:
        return values[0], "PRODUCTION_LOG_FIELD_SINGLE_VALUE"
    if all(value == values[0] for value in values[1:]):
        return values[0], "PRODUCTION_LOG_FIELD_IDENTICAL_REPEATS"

    concrete_indexes = tuple(
        index
        for index, value in enumerate(values)
        if not _is_placeholder(value, none_is_placeholder=none_is_placeholder)
    )
    if concrete_indexes == (len(values) - 1,):
        return values[-1], "PRODUCTION_LOG_FIELD_PLACEHOLDER_THEN_CONCRETE"
    return None, "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS"


def _is_placeholder(value: str, *, none_is_placeholder: bool) -> bool:
    return (
        value == "UNAVAILABLE"
        or value.startswith("UNAVAILABLE_")
        or (none_is_placeholder and value == "NONE")
    )
