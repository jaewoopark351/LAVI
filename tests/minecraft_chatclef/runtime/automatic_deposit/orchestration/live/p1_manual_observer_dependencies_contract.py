#20260901_kpopmodder: Verify only the eight declared read-only P1 capabilities.
from __future__ import annotations

from .p1_manual_observer_dependencies import P1ManualObserverDependencies


def missing_p1_manual_observer_dependencies(
    dependencies: P1ManualObserverDependencies,
) -> tuple[str, ...]:
    return tuple(
        name
        for name in sorted(dependencies.__slots__)
        if not callable(getattr(dependencies, name))
    )
