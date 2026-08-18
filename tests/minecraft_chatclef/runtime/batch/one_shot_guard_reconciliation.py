#20260818_kpopmodder: Adapt verified batch steps to the matching one-shot reconciler.
from __future__ import annotations

from collections.abc import Mapping

from ..submission.one_shot_run_reconciler import OneShotRunReconciler


def reconcile_batch_one_shot_guard(
    environment: Mapping[str, object],
    _command_result: Mapping[str, object],
) -> dict[str, object]:
    reconciler = OneShotRunReconciler(str(environment.get("repository_root") or ""))
    return reconciler.reconcile(str(environment.get("invocation_id") or ""))
