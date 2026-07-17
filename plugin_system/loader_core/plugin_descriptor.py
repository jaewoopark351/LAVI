#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass

from plugin_system.contracts_core.availability_probe_contract import (
    AvailabilityProbeContract,
)
from plugin_system.contracts_core.plugin_runtime_contract import PluginRuntimeContract
from plugin_system.contracts_core.plugin_supports import PluginSupports


@dataclass(frozen=True)
class PluginDescriptor:
    #20260716_kpopmodder: Describes a provider discovered without importing it.
    plugin_name: str
    class_name: str
    category: str
    interface_name: str
    module_name: str
    module_path: str
    id: str = ""
    display_name: str = ""
    api_version: str = "1"
    dependency_group: str = ""
    capabilities: tuple = ()
    config_schema: dict = None
    required_python_packages: tuple = ()
    required_files: tuple = ()
    required_executables: tuple = ()
    required_services: tuple = ()
    availability_probe_timeout_sec: float = 0.25
    availability_probe_log_reference: str = ""
    supports_offline: bool = False
    supports_cpu: bool = True
    requires_gpu: bool = False

    @property
    def status_key(self):
        return f"{self.plugin_name}.{self.class_name}"

    @property
    def entrypoint(self):
        return f"{self.module_name}:{self.class_name}"

    @property
    def runtime_contract(self):
        return PluginRuntimeContract(
            plugin_id=self.id,
            manifest={
                "id": self.id,
                "display_name": self.display_name,
                "api_version": self.api_version,
                "category": self.category,
                "entrypoint": self.entrypoint,
                "dependency_group": self.dependency_group,
            },
            config_schema=self.config_schema,
            availability_probe=AvailabilityProbeContract(
                required_python_packages=self.required_python_packages,
                required_files=self.required_files,
                required_executables=self.required_executables,
                required_services=self.required_services,
                timeout_sec=self.availability_probe_timeout_sec,
                log_reference=(
                    self.availability_probe_log_reference
                    or f"PluginLoader availability probe for {self.status_key}"
                ),
            ),
            capabilities=self.capabilities,
            supports=PluginSupports(
                offline=self.supports_offline,
                cpu=self.supports_cpu,
                requires_gpu=self.requires_gpu,
            ),
        )

    def __post_init__(self):
        if self.config_schema is None:
            object.__setattr__(self, "config_schema", {})
        if not self.id:
            object.__setattr__(self, "id", self.class_name)
        if not self.display_name:
            object.__setattr__(self, "display_name", self.class_name)
        if not self.dependency_group:
            object.__setattr__(self, "dependency_group", self.category)
