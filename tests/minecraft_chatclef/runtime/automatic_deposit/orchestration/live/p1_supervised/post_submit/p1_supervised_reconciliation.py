#20260901_kpopmodder: Reconcile only one exact invocation and command fingerprint after verified evidence.
from __future__ import annotations

from collections.abc import Mapping


def reconcile_verified_p1_supervised_run(
    reconciler: object,
    invocation_id: str,
    expected_command_fingerprint: str,
) -> Mapping[str, object]:
    if not callable(reconciler):
        return {}
    try:
        result = reconciler(invocation_id, expected_command_fingerprint)
    except Exception:
        return {}
    return result if isinstance(result, Mapping) else {}
