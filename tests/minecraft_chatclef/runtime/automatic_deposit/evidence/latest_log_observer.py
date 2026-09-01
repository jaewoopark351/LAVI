#20260831_kpopmodder: Fix product log observation to the shared read-only byte-cursor reader.
from __future__ import annotations

from .latest_log_cursor_reader import read_automatic_deposit_latest_log_delta
from .latest_log_delta_result import AutomaticDepositLatestLogDeltaResult
from .run_manifest import AutomaticDepositRunManifest


def collect_automatic_deposit_latest_log_delta(
    run_manifest: object,
) -> tuple[AutomaticDepositLatestLogDeltaResult | None, str]:
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        return None, "RUN_MANIFEST_NOT_TYPED"
    delta = read_automatic_deposit_latest_log_delta(
        run_manifest.latest_log_cursor
    )
    return delta, delta.reason
