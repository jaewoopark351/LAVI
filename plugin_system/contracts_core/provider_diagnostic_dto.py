#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass

from plugin_system.contracts_core.contract_helpers import (
    _contract_to_dict,
    _diagnostic_to_dict,
)


@dataclass(frozen=True)
class ProviderDiagnosticDTO:
    #20260717_kpopmodder: Typed provider-selection diagnostics returned by PluginSelection.
    plugin_id: str
    name: str
    category: str
    state: str
    detail: str = ""
    diagnostic: object = None
    runtime_contract: object = None
    selected: bool = False
    initialized: bool = False
    disabled: bool = False

    @classmethod
    def from_snapshot(
        cls,
        snapshot,
        *,
        selected=False,
        initialized=False,
        disabled=False,
    ):
        snapshot = dict(snapshot or {})
        return cls(
            plugin_id=str(snapshot.get("plugin_id", "")),
            name=str(snapshot.get("name", "")),
            category=str(snapshot.get("category", "")),
            state=str(snapshot.get("state", "")),
            detail=str(snapshot.get("detail", "")),
            diagnostic=snapshot.get("diagnostic") or {},
            runtime_contract=snapshot.get("runtime_contract") or {},
            selected=bool(selected),
            initialized=bool(initialized),
            disabled=bool(disabled),
        )

    def to_dict(self):
        return {
            "plugin_id": self.plugin_id,
            "name": self.name,
            "category": self.category,
            "state": self.state,
            "detail": self.detail,
            "diagnostic": _diagnostic_to_dict(self.diagnostic),
            "runtime_contract": _contract_to_dict(self.runtime_contract),
            "selected": self.selected,
            "initialized": self.initialized,
            "disabled": self.disabled,
        }
