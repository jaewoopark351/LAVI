#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass, field

from plugin_system.contracts_core.availability_probe_contract import (
    AvailabilityProbeContract,
)
from plugin_system.contracts_core.contract_constants import (
    PLUGIN_AVAILABILITY_PROBE_FIELDS,
    PLUGIN_LIFECYCLE_METHODS,
    PLUGIN_MANIFEST_FIELDS,
)
from plugin_system.contracts_core.contract_helpers import (
    _add_text_sequence_issues,
    _is_non_empty_text,
)
from plugin_system.contracts_core.plugin_contract_issue import PluginContractIssue
from plugin_system.contracts_core.plugin_supports import PluginSupports


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
