#20260717_kpopmodder: Added this module to keep one project class per Python file.
from plugin_system.selection_core.selection_requirements import (
    _coerce_runtime_requirements,
)


class Provider():
    #20260630_kpopmodder: Track lazy init state per provider so startup does not load every backend.
    def __init__(self):
        self.plugin = None
        self.handle = None
        self.name = ""
        self.ui = None
        self.initialized = False
        self.disabled = False
        self.init_error = ""
        self.needs_shutdown = False
        self.shutdown_attempted = False

    #20260717_kpopmodder
    @property
    def runtime_contract(self):
        contract = getattr(self.handle, "runtime_contract", None)
        if contract is not None:
            return contract
        contract = getattr(self.plugin, "runtime_contract", None)
        if callable(contract):
            return contract()
        return contract

    def runtime_contract_dict(self):
        contract = self.runtime_contract
        if hasattr(contract, "to_dict"):
            return contract.to_dict()
        if isinstance(contract, dict):
            return dict(contract)
        return {}

    def matches_requirements(self, requirements):
        requirements = _coerce_runtime_requirements(requirements)
        return requirements.matches(self.runtime_contract)
