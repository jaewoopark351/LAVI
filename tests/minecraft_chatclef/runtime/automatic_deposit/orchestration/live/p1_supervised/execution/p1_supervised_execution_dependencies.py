#20260901_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class P1SupervisedExecutionDependencies:
    gateway_factory: Callable[[str], object]
    runner: Callable[..., object]
