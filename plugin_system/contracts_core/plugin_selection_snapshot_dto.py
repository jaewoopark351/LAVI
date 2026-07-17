#20260718_kpopmodder: Added typed plugin selection snapshot for provider result boundaries.
from dataclasses import dataclass
from typing import Tuple

from plugin_system.contracts_core.contract_helpers import _contract_to_dict


@dataclass(frozen=True)
class PluginSelectionSnapshotDTO:
    #20260718_kpopmodder: Keeps PluginSelection state typed while legacy callers can still use dicts.
    category: str
    selected_provider: str
    default_provider: str
    default_provider_source: str
    available_providers: Tuple[str, ...] = ()
    provider_diagnostics: Tuple[object, ...] = ()
    runtime_requirements: object = None

    def to_dict(self):
        return {
            "category": self.category,
            "selected_provider": self.selected_provider,
            "default_provider": self.default_provider,
            "default_provider_source": self.default_provider_source,
            "available_providers": list(self.available_providers),
            "provider_diagnostics": [
                item.to_dict() if hasattr(item, "to_dict") else dict(item or {})
                for item in self.provider_diagnostics
            ],
            "runtime_requirements": _contract_to_dict(self.runtime_requirements),
        }
