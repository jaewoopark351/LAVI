#20260716_kpopmodder: Compatibility exports for plugin runtime contracts.
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
    _contract_to_dict,
    _diagnostic_to_dict,
    _is_non_empty_text,
    _text_tuple,
    validate_plugin_lifecycle,
)
from plugin_system.contracts_core.plugin_contract_issue import PluginContractIssue
from plugin_system.contracts_core.plugin_diagnostic import PluginDiagnostic
from plugin_system.contracts_core.plugin_diagnostic_snapshot import (
    PluginDiagnosticSnapshot,
)
from plugin_system.contracts_core.plugin_runtime_contract import PluginRuntimeContract
from plugin_system.contracts_core.plugin_runtime_requirements import (
    PluginRuntimeRequirements,
)
from plugin_system.contracts_core.plugin_selection_snapshot_dto import (
    PluginSelectionSnapshotDTO,
)
from plugin_system.contracts_core.plugin_state import PluginState
from plugin_system.contracts_core.plugin_supports import PluginSupports
from plugin_system.contracts_core.provider_diagnostic_dto import ProviderDiagnosticDTO


__all__ = [
    "PLUGIN_AVAILABILITY_PROBE_FIELDS",
    "PLUGIN_LIFECYCLE_METHODS",
    "PLUGIN_MANIFEST_FIELDS",
    "AvailabilityProbeContract",
    "PluginContractIssue",
    "PluginDiagnostic",
    "PluginDiagnosticSnapshot",
    "PluginRuntimeContract",
    "PluginRuntimeRequirements",
    "PluginSelectionSnapshotDTO",
    "PluginState",
    "PluginSupports",
    "ProviderDiagnosticDTO",
    "_add_text_sequence_issues",
    "_contract_to_dict",
    "_diagnostic_to_dict",
    "_is_non_empty_text",
    "_text_tuple",
    "validate_plugin_lifecycle",
]
