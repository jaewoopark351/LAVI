#20260819_kpopmodder: Validate an optional batch-bound process fingerprint.
from __future__ import annotations

from collections.abc import Mapping


EXPECTED_PROCESS_IDENTITY_FIELD = "expected_process_identity_fingerprint"


def expected_process_identity_fingerprint(
    environment: Mapping[str, object],
) -> tuple[str | None, str]:
    if EXPECTED_PROCESS_IDENTITY_FIELD not in environment:
        return None, ""
    value = environment.get(EXPECTED_PROCESS_IDENTITY_FIELD)
    if (
        type(value) is not str
        or len(value) != 64
        or value != value.strip().lower()
        or any(character not in "0123456789abcdef" for character in value)
    ):
        return None, "batch process identity fingerprint is invalid"
    return value, ""
