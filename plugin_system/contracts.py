#20260716_kpopmodder: Shared plugin runtime contracts used across loader, selection, and future adapters.
from dataclasses import dataclass, field


PLUGIN_LIFECYCLE_METHODS = ("init", "start", "stop", "shutdown")


def _diagnostic_to_dict(value):
    if value is None:
        return {}
    if hasattr(value, "to_dict"):
        return value.to_dict()
    if isinstance(value, dict):
        return dict(value)
    return {"message": str(value)}


class PluginState:
    #20260716_kpopmodder: Canonical lifecycle state names for discovered providers.
    DISABLED = "DISABLED"
    UNAVAILABLE = "UNAVAILABLE"
    READY = "READY"
    STARTING = "STARTING"
    RUNNING = "RUNNING"
    FAILED = "FAILED"
    STOPPED = "STOPPED"
    BROKEN = FAILED  # Backward-compatible alias for older tests/log checks.


@dataclass(frozen=True)
class PluginSupports:
    #20260716_kpopmodder: Runtime capability flags separated from plugin metadata parsing.
    offline: bool = False
    cpu: bool = True
    requires_gpu: bool = False

    def to_dict(self):
        return {
            "offline": self.offline,
            "cpu": self.cpu,
            "requires_gpu": self.requires_gpu,
        }


@dataclass(frozen=True)
class AvailabilityProbeContract:
    #20260716_kpopmodder: Declarative probe inputs; actual probing stays owned by adapters/loader.
    required_python_packages: tuple = ()
    required_files: tuple = ()
    required_executables: tuple = ()
    required_services: tuple = ()
    timeout_sec: float = 0.25
    log_reference: str = ""

    def to_dict(self):
        return {
            "required_python_packages": list(self.required_python_packages),
            "required_files": list(self.required_files),
            "required_executables": list(self.required_executables),
            "required_services": list(self.required_services),
            "timeout_sec": self.timeout_sec,
            "log_reference": self.log_reference,
        }


@dataclass(frozen=True)
class PluginDiagnostic:
    #20260716_kpopmodder: Structured plugin diagnostic without exposing credential values.
    plugin_id: str
    state: str
    reason_code: str
    human_readable_message: str
    missing_python_packages: tuple = ()
    missing_files: tuple = ()
    missing_executables: tuple = ()
    missing_services: tuple = ()
    missing_environment_variables: tuple = ()
    suggested_install_profile: str = ""
    suggested_command: str = ""
    log_reference: str = ""

    def to_dict(self):
        return {
            "plugin_id": self.plugin_id,
            "state": self.state,
            "reason_code": self.reason_code,
            "human_readable_message": self.human_readable_message,
            "missing_python_packages": list(self.missing_python_packages),
            "missing_files": list(self.missing_files),
            "missing_executables": list(self.missing_executables),
            "missing_services": list(self.missing_services),
            "missing_environment_variables": list(
                self.missing_environment_variables
            ),
            "suggested_install_profile": self.suggested_install_profile,
            "suggested_command": self.suggested_command,
            "log_reference": self.log_reference,
        }


@dataclass(frozen=True)
class PluginRuntimeContract:
    #20260716_kpopmodder: Single shape for future plugin manifests and adapter contracts.
    plugin_id: str
    manifest: dict = field(default_factory=dict)
    config_schema: dict = field(default_factory=dict)
    availability_probe: AvailabilityProbeContract = field(
        default_factory=AvailabilityProbeContract
    )
    lifecycle_methods: tuple = PLUGIN_LIFECYCLE_METHODS
    capabilities: tuple = ()
    supports: PluginSupports = field(default_factory=PluginSupports)

    def to_dict(self):
        return {
            "plugin_id": self.plugin_id,
            "manifest": dict(self.manifest),
            "config_schema": dict(self.config_schema),
            "availability_probe": self.availability_probe.to_dict(),
            "lifecycle_methods": list(self.lifecycle_methods),
            "capabilities": list(self.capabilities),
            "supports": self.supports.to_dict(),
        }


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
