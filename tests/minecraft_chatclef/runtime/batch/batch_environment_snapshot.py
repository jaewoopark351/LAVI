#20260819_kpopmodder: Expose one read-only snapshot to batch callbacks.
from __future__ import annotations

from collections.abc import Mapping
from types import MappingProxyType


def batch_environment_snapshot(
    environment: Mapping[str, object],
) -> Mapping[str, object]:
    return MappingProxyType(dict(environment))
