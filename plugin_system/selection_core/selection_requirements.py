#20260717_kpopmodder: Added this module to keep provider requirement helpers class-free.
from plugin_system.contracts_core.plugin_runtime_requirements import (
    PluginRuntimeRequirements,
)


def _coerce_runtime_requirements(requirements=None, **overrides):
    if isinstance(requirements, PluginRuntimeRequirements):
        base = requirements
    elif isinstance(requirements, dict):
        base = PluginRuntimeRequirements(**requirements)
    elif requirements is None:
        base = PluginRuntimeRequirements()
    else:
        raise TypeError("runtime requirements must be a PluginRuntimeRequirements or dict")

    values = base.to_dict()
    for key, value in overrides.items():
        if value is not None:
            values[key] = value
    return PluginRuntimeRequirements(**values)
