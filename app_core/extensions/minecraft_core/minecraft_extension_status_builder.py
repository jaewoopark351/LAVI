#20260725_kpopmodder: Added Minecraft extension status builder separate from lifecycle control.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_extension_status_snapshot import MinecraftExtensionStatusSnapshot
from .minecraft_plugin_status_reader import MinecraftPluginStatusReader
from .minecraft_runtime_context_snapshot_reader import MinecraftRuntimeContextSnapshotReader


class MinecraftExtensionStatusBuilder:
    def __init__(
        self,
        plugin_status_reader: MinecraftPluginStatusReader | None = None,
        extension_status_snapshot: MinecraftExtensionStatusSnapshot | None = None,
        runtime_context_snapshot_reader: MinecraftRuntimeContextSnapshotReader | None = None,
    ):
        self.plugin_status_reader = plugin_status_reader or MinecraftPluginStatusReader()
        self.extension_status_snapshot = (
            extension_status_snapshot or MinecraftExtensionStatusSnapshot()
        )
        self.runtime_context_snapshot_reader = (
            runtime_context_snapshot_reader or MinecraftRuntimeContextSnapshotReader()
        )

    def build(
        self,
        *,
        name: str,
        plugin: Any,
        runtime_context: Any,
        initialized: bool,
        started: bool,
    ) -> Dict[str, Any]:
        plugin_status = self.plugin_status_reader.read(plugin)
        plugin_status["extension"] = self.extension_status_snapshot.build(
            name=name,
            initialized=initialized,
            started=started,
        )
        runtime_snapshot = self.runtime_context_snapshot_reader.read(runtime_context)
        if runtime_snapshot is not None:
            plugin_status["runtime_context"] = runtime_snapshot
        return plugin_status
