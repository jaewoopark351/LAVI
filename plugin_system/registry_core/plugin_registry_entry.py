#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass, field


@dataclass
class PluginRegistryEntry:
    #20260716_kpopmodder: Keep registry entries simple and serializable for diagnostics.
    name: str
    status: str
    kind: str = "core"
    detail: str = ""
    diagnostic: dict = field(default_factory=dict)
    runtime_contract: dict = field(default_factory=dict)
