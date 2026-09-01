#20260901_kpopmodder: Define only read-only observer capabilities for manual P1.
from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass

from ...evidence.latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
)


ReadOnlyObservationReader = Callable[[], tuple[object | None, str]]
RuntimeArtifactObservationReader = Callable[
    [AutomaticDepositLatestLogByteCursor],
    tuple[object | None, str],
]


@dataclass(frozen=True, slots=True)
class P1ManualObserverDependencies:
    status_reader: ReadOnlyObservationReader | None = None
    runtime_artifact_reader: RuntimeArtifactObservationReader | None = None
    artifact_recheck_reader: ReadOnlyObservationReader | None = None
    carry_on_recheck_reader: ReadOnlyObservationReader | None = None
    trusted_fixture_reader: ReadOnlyObservationReader | None = None
    operator_action_reader: ReadOnlyObservationReader | None = None
    log_delta_reader: ReadOnlyObservationReader | None = None
    gameplay_reader: ReadOnlyObservationReader | None = None
