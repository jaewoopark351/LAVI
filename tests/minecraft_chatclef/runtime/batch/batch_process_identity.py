#20260819_kpopmodder: Bind one verified listener identity across batch steps.
from __future__ import annotations

from collections.abc import Mapping

from ..preflight.windows_listener.process_identity.process_identity_key import (
    process_identity_key,
)


EXPECTED_PROCESS_IDENTITY_FIELD = "expected_process_identity_fingerprint"


def batch_process_identity_key(command_result: Mapping[str, object]) -> str | None:
    preflight = command_result.get("preflight")
    if not isinstance(preflight, Mapping):
        return None
    observed = preflight.get("observed")
    if not isinstance(observed, Mapping):
        return None
    return process_identity_key(observed)


def bind_expected_process_identity(
    environment: Mapping[str, object],
    process_key: str | None,
) -> dict[str, object]:
    bound = dict(environment)
    if process_key is not None:
        bound[EXPECTED_PROCESS_IDENTITY_FIELD] = process_key
    return bound
