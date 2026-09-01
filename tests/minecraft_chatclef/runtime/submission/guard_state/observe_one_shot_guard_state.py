#20260901_kpopmodder: Expose one simple read-only guard observation entry point.
from __future__ import annotations

from collections.abc import Callable
from pathlib import Path

from .one_shot_guard_state_observation import OneShotGuardStateObservation
from .one_shot_guard_state_observer import OneShotGuardStateObserver


def observe_one_shot_guard_state(
    repository_root: str,
    *,
    state_directory: str | Path | None = None,
    record_exists: Callable[[Path], bool] | None = None,
    record_reader: Callable[[Path], object] | None = None,
) -> tuple[OneShotGuardStateObservation, str]:
    observation = OneShotGuardStateObserver(
        repository_root,
        state_directory=state_directory,
        record_exists=record_exists,
        record_reader=record_reader,
    ).observe()
    return observation, observation.reason
