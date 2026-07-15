#20260716_kpopmodder: Shared plugin status registry for core and optional providers.
from dataclasses import dataclass


@dataclass
class PluginRegistryEntry:
    #20260716_kpopmodder: Keep registry entries simple and serializable for diagnostics.
    name: str
    status: str
    kind: str = "core"
    detail: str = ""


class PluginRegistry:
    #20260716_kpopmodder: Central place for plugin loader and optional manifest status.
    def __init__(self):
        self._entries = {}

    def record(self, name, status, kind="core", detail=""):
        self._entries[name] = PluginRegistryEntry(
            name=name,
            status=status,
            kind=kind,
            detail=str(detail or ""),
        )

    def snapshot(self):
        return {
            name: {
                "status": entry.status,
                "kind": entry.kind,
                "detail": entry.detail,
            }
            for name, entry in sorted(self._entries.items())
        }


plugin_registry = PluginRegistry()
