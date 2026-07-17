#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass

from plugin_system.contracts_core.contract_helpers import _diagnostic_to_dict
from plugin_system.contracts_core.plugin_runtime_contract import PluginRuntimeContract


@dataclass(frozen=True)
class PluginDiagnosticSnapshot:
    #20260716_kpopmodder: Serializable state view that pairs diagnostics with the plugin contract.
    plugin_id: str
    name: str
    category: str
    state: str
    detail: str = ""
    diagnostic: object = None
    runtime_contract: PluginRuntimeContract = None

    def to_dict(self):
        runtime_contract = {}
        if self.runtime_contract is not None:
            runtime_contract = self.runtime_contract.to_dict()
        return {
            "plugin_id": self.plugin_id,
            "name": self.name,
            "category": self.category,
            "state": self.state,
            "detail": self.detail,
            "diagnostic": _diagnostic_to_dict(self.diagnostic),
            "runtime_contract": runtime_contract,
        }
