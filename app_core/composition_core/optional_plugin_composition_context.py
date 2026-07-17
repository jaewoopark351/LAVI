#20260717_kpopmodder: Isolates optional plugin composition inputs as a typed DTO.
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class OptionalPluginCompositionContext:
    #20260717_kpopmodder: Typed DTO for optional plugin construction inputs.
    current_module_directory: str
    memory_store: Any = None
