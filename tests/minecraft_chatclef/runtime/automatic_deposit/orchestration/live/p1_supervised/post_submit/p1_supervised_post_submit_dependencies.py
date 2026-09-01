#20260901_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class P1SupervisedPostSubmitDependencies:
    log_delta_reader: Callable[[object], tuple[object | None, str]]
    gameplay_observation_reader: Callable[
        [object, object], tuple[object | None, str]
    ]
    reconciler: Callable[[str, str], object]
