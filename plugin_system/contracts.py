#20260716_kpopmodder: Shared plugin runtime contracts used across loader, selection, and future adapters.
from dataclasses import dataclass, field


PLUGIN_LIFECYCLE_METHODS = ("init", "start", "stop", "shutdown")
PLUGIN_MANIFEST_FIELDS = (
    "id",
    "display_name",
    "api_version",
    "category",
    "entrypoint",
    "dependency_group",
)
PLUGIN_AVAILABILITY_PROBE_FIELDS = (
    "required_python_packages",
    "required_files",
    "required_executables",
    "required_services",
)


def _is_non_empty_text(value):
    return isinstance(value, str) and bool(value.strip())


def _add_text_sequence_issues(issues, value, path):
    if not isinstance(value, (list, tuple)):
        issues.append(PluginContractIssue(
            code="contract_invalid_sequence",
            message=f"{path} must be a list or tuple of strings",
            path=path,
        ))
        return
    for item in value:
        if not _is_non_empty_text(item):
            issues.append(PluginContractIssue(
                code="contract_invalid_sequence_item",
                message=f"{path} entries must be non-empty strings",
                path=path,
            ))


def validate_plugin_lifecycle(target, plugin_id="", required_methods=PLUGIN_LIFECYCLE_METHODS):
    #20260717_kpopmodder: Runtime plugin instances must expose the common lifecycle surface.
    issues = []
    target_name = getattr(target, "__name__", target.__class__.__name__)
    label = str(plugin_id or target_name or "plugin")
    for method_name in required_methods:
        method = getattr(target, method_name, None)
        if callable(method):
            continue
        issues.append(PluginContractIssue(
            code="contract_missing_lifecycle_callable",
            message=f"{label}.{method_name} must be callable",
            path=f"lifecycle.{method_name}",
        ))
    return tuple(issues)


@dataclass(frozen=True)
class PluginContractIssue:
    #20260717_kpopmodder: Typed issue DTO for validating plugin runtime contracts.
    code: str
    message: str
    path: str = ""
    severity: str = "error"

    def to_dict(self):
        return {
            "code": self.code,
            "message": self.message,
            "path": self.path,
            "severity": self.severity,
        }


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

    def validate(self):
        issues = []
        if not _is_non_empty_text(self.plugin_id):
            issues.append(PluginContractIssue(
                code="contract_missing_plugin_id",
                message="plugin_id must be a non-empty string",
                path="plugin_id",
            ))

        if not isinstance(self.manifest, dict):
            issues.append(PluginContractIssue(
                code="contract_invalid_manifest",
                message="manifest must be an object",
                path="manifest",
            ))
            manifest = {}
        else:
            manifest = self.manifest

        for field_name in PLUGIN_MANIFEST_FIELDS:
            if not _is_non_empty_text(manifest.get(field_name)):
                issues.append(PluginContractIssue(
                    code="contract_missing_manifest_field",
                    message=f"manifest.{field_name} must be a non-empty string",
                    path=f"manifest.{field_name}",
                ))

        manifest_id = manifest.get("id")
        if (
            _is_non_empty_text(self.plugin_id)
            and _is_non_empty_text(manifest_id)
            and self.plugin_id.strip() != manifest_id.strip()
        ):
            issues.append(PluginContractIssue(
                code="contract_manifest_id_mismatch",
                message="plugin_id must match manifest.id",
                path="manifest.id",
            ))

        if not isinstance(self.config_schema, dict):
            issues.append(PluginContractIssue(
                code="contract_invalid_config_schema",
                message="config_schema must be an object",
                path="config_schema",
            ))

        if not isinstance(self.availability_probe, AvailabilityProbeContract):
            issues.append(PluginContractIssue(
                code="contract_invalid_availability_probe",
                message="availability_probe must be an AvailabilityProbeContract",
                path="availability_probe",
            ))
        else:
            for field_name in PLUGIN_AVAILABILITY_PROBE_FIELDS:
                _add_text_sequence_issues(
                    issues,
                    getattr(self.availability_probe, field_name),
                    f"availability_probe.{field_name}",
                )
            timeout_sec = self.availability_probe.timeout_sec
            if (
                isinstance(timeout_sec, bool)
                or not isinstance(timeout_sec, (int, float))
                or timeout_sec <= 0
            ):
                issues.append(PluginContractIssue(
                    code="contract_invalid_availability_timeout",
                    message="availability_probe.timeout_sec must be a positive number",
                    path="availability_probe.timeout_sec",
                ))
            if not isinstance(self.availability_probe.log_reference, str):
                issues.append(PluginContractIssue(
                    code="contract_invalid_log_reference",
                    message="availability_probe.log_reference must be a string",
                    path="availability_probe.log_reference",
                ))

        _add_text_sequence_issues(
            issues,
            self.lifecycle_methods,
            "lifecycle_methods",
        )
        if isinstance(self.lifecycle_methods, (list, tuple)):
            lifecycle_methods = tuple(self.lifecycle_methods)
            for method_name in PLUGIN_LIFECYCLE_METHODS:
                if method_name not in lifecycle_methods:
                    issues.append(PluginContractIssue(
                        code="contract_missing_lifecycle_method",
                        message=f"lifecycle_methods must include {method_name}",
                        path="lifecycle_methods",
                    ))

        if self.capabilities:
            _add_text_sequence_issues(
                issues,
                self.capabilities,
                "capabilities",
            )
        else:
            issues.append(PluginContractIssue(
                code="contract_missing_capabilities",
                message="capabilities should describe what the plugin provides",
                path="capabilities",
                severity="warning",
            ))

        if not isinstance(self.supports, PluginSupports):
            issues.append(PluginContractIssue(
                code="contract_invalid_supports",
                message="supports must be a PluginSupports instance",
                path="supports",
            ))

        return tuple(issues)

    def validation_errors(self):
        return tuple(
            issue for issue in self.validate()
            if issue.severity == "error"
        )

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
