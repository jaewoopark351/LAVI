#20260718_kpopmodder: Added typed module settings snapshot for active plugin combination checks.
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping, Optional, Tuple


@dataclass(frozen=True)
class ModuleSettingsSnapshot:
    #20260718_kpopmodder: Keeps modules.json config access typed while preserving legacy dict loading.
    settings: Mapping[str, bool]
    path: Optional[Path] = None
    source: str = ""
    profile: str = ""

    @classmethod
    def from_resolution(cls, resolution):
        return cls(
            settings=dict(getattr(resolution, "settings", {}) or {}),
            path=getattr(resolution, "path", None),
            source=str(getattr(resolution, "source", "") or ""),
            profile=str(getattr(resolution, "profile", "") or ""),
        )

    def has_module(self, module_name: str) -> bool:
        return str(module_name or "") in self.settings

    def is_enabled(self, module_name: str) -> bool:
        return self.settings.get(str(module_name or "")) is True

    def enabled_modules(self) -> Tuple[str, ...]:
        return tuple(
            name
            for name, enabled in self.settings.items()
            if enabled is True
        )

    def disabled_modules(self) -> Tuple[str, ...]:
        return tuple(
            name
            for name, enabled in self.settings.items()
            if enabled is not True
        )

    def to_dict(self):
        return {
            "path": str(self.path) if self.path is not None else "",
            "source": self.source,
            "profile": self.profile,
            "settings": dict(self.settings),
            "enabled_modules": list(self.enabled_modules()),
            "disabled_modules": list(self.disabled_modules()),
        }
