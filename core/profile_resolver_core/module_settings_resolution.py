#20260717_kpopmodder: Isolates typed module settings resolution result.
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ModuleSettingsResolution:
    path: Path
    source: str
    profile: str
    settings: dict

    def snapshot(self):
        #20260718_kpopmodder: Expose a typed config DTO without changing legacy settings access.
        from core.profile_resolver_core.module_settings_snapshot import (
            ModuleSettingsSnapshot,
        )

        return ModuleSettingsSnapshot.from_resolution(self)
