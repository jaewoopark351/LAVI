# 20260905_kpopmodder: Strictly parse one canonical space-delimited diagnostic record.
from __future__ import annotations


def parse_canonical_diagnostic(
    message: object,
    *,
    event_name: str,
    canonical_fields: tuple[str, ...],
) -> dict[str, str]:
    if type(message) is not str:
        raise AssertionError("diagnostic must be an exact str")
    parts = message.split(" ")
    if not parts or parts[0] != f"event={event_name}":
        raise AssertionError(f"unexpected diagnostic event: {message!r}")
    pairs = parts[1:]
    names = []
    parsed = {}
    for pair in pairs:
        if pair.count("=") != 1:
            raise AssertionError(f"invalid diagnostic atom: {pair!r}")
        name, value = pair.split("=", 1)
        if not name or not value or name in parsed:
            raise AssertionError(f"duplicate or empty diagnostic field: {pair!r}")
        names.append(name)
        parsed[name] = value
    if tuple(names) != canonical_fields:
        raise AssertionError(
            f"canonical field mismatch: expected={canonical_fields!r} actual={tuple(names)!r}"
        )
    return parsed


__all__ = ["parse_canonical_diagnostic"]
