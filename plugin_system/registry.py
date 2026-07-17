#20260716_kpopmodder: Shared plugin status registry for core and optional providers.
from plugin_system.registry_core.plugin_registry_entry import PluginRegistryEntry


class PluginRegistry:
    #20260716_kpopmodder: Central place for plugin loader and optional manifest status.
    def __init__(self):
        self._entries = {}

    def record(
        self,
        name,
        status,
        kind="core",
        detail="",
        diagnostic=None,
        runtime_contract=None,
    ):
        self._entries[name] = PluginRegistryEntry(
            name=name,
            status=status,
            kind=kind,
            detail=str(detail or ""),
            diagnostic=dict(diagnostic or {}),
            runtime_contract=dict(runtime_contract or {}),
        )

    def snapshot(self):
        return {
            name: {
                "status": entry.status,
                "kind": entry.kind,
                "detail": entry.detail,
                "diagnostic": entry.diagnostic,
                "runtime_contract": entry.runtime_contract,
            }
            for name, entry in sorted(self._entries.items())
        }


plugin_registry = PluginRegistry()
