#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass

from plugin_system.contracts_core.contract_helpers import (
    _contract_to_dict,
    _text_tuple,
)


@dataclass(frozen=True)
class PluginRuntimeRequirements:
    #20260717_kpopmodder
    required_capabilities: tuple = ()
    supports_offline: object = None
    supports_cpu: object = None
    requires_gpu: object = None

    def __post_init__(self):
        object.__setattr__(
            self,
            "required_capabilities",
            _text_tuple(self.required_capabilities),
        )

    def is_empty(self):
        return not (
            self.required_capabilities
            or self.supports_offline is not None
            or self.supports_cpu is not None
            or self.requires_gpu is not None
        )

    def matches(self, runtime_contract):
        if self.is_empty():
            return True

        contract = _contract_to_dict(runtime_contract)
        if not contract:
            return False

        capabilities = set(_text_tuple(contract.get("capabilities")))
        for capability in self.required_capabilities:
            if capability not in capabilities:
                return False

        supports = contract.get("supports") or {}
        conditions = (
            ("offline", self.supports_offline),
            ("cpu", self.supports_cpu),
            ("requires_gpu", self.requires_gpu),
        )
        for key, expected in conditions:
            if expected is None:
                continue
            if supports.get(key) is not expected:
                return False

        return True

    def to_dict(self):
        return {
            "required_capabilities": list(self.required_capabilities),
            "supports_offline": self.supports_offline,
            "supports_cpu": self.supports_cpu,
            "requires_gpu": self.requires_gpu,
        }
