#20260818_kpopmodder: Preserve the listener-owner entrypoint compatibility function.
#20260819_kpopmodder: Delegate legacy entrypoint checks to structured process identity.
from __future__ import annotations

from typing import Mapping

from .process_identity.process_ancestry import inspect_approved_process_identity


def approved_entrypoint(
    process_id: int,
    processes: Mapping[int, Mapping[str, object]],
    repository_root: str,
) -> str:
    result = inspect_approved_process_identity(process_id, processes, repository_root)
    return str(result.get("entrypoint") or "") if result.get("ok") else ""
