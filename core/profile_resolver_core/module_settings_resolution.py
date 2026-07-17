#20260717_kpopmodder: Isolates typed module settings resolution result.
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ModuleSettingsResolution:
    path: Path
    source: str
    profile: str
    settings: dict
