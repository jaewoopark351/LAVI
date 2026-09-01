#20260901_kpopmodder: Accept only the exact sealed guard observer return shape.
from __future__ import annotations

from collections.abc import Callable

from minecraft_chatclef.runtime.submission.guard_state import (
    OneShotGuardStateObservation,
)

from .p1_guard_observation_result import P1GuardObservationResult


def read_p1_guard_observation(
    repository_root: str,
    reader: Callable[[str], object],
) -> P1GuardObservationResult:
    try:
        raw = reader(repository_root)
    except Exception as error:
        return P1GuardObservationResult(None, type(error).__name__)
    if type(raw) is not tuple or len(raw) != 2:
        return P1GuardObservationResult(None)
    observation, source_reason = raw
    if not isinstance(observation, OneShotGuardStateObservation):
        return P1GuardObservationResult(None)
    if not _exact_reason(source_reason, observation.reason):
        return P1GuardObservationResult(None)
    return P1GuardObservationResult(observation)


def _exact_reason(source_reason: object, observed_reason: str) -> bool:
    return (
        isinstance(source_reason, str)
        and source_reason == observed_reason
        and source_reason == source_reason.strip()
        and 0 < len(source_reason) <= 512
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F
            for character in source_reason
        )
    )
