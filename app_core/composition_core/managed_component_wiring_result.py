#20260717_kpopmodder: Isolates RuntimeLifecycle wiring DTO from the wiring service.
from dataclasses import dataclass
from typing import Any, Tuple


@dataclass(frozen=True)
class ManagedComponentWiringResult:
    #20260717_kpopmodder: Typed result for RuntimeLifecycle component lists.
    managed_components: Tuple[Any, ...]
    core_components: Tuple[Any, ...]
    optional_components: Tuple[Any, ...]
    startup_components: Tuple[Any, ...] = ()
