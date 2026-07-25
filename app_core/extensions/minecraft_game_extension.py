#20260725_kpopmodder: Added GameExtension adapter for Minecraft ChatClef bridge commands.
from __future__ import annotations

from typing import Any, Dict, Optional

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.game_extension_interface import GameExtensionInterface
from app_core.extensions.minecraft_core import (
    MinecraftCommandDispatcher,
    MinecraftExtensionStatusBuilder,
    MinecraftPluginLoader,
    MinecraftRuntimeResourceSyncer,
)
from core.logger import log_print


class MinecraftGameExtension(GameExtensionInterface):
    def __init__(self, plugin=None):
        self.plugin = plugin
        self._context: Optional[GameExtensionContext] = None
        self._is_initialized = False
        self._is_started = False
        self.plugin_loader = MinecraftPluginLoader()
        self.resource_syncer = MinecraftRuntimeResourceSyncer()
        self.command_dispatcher = MinecraftCommandDispatcher()
        self.status_builder = MinecraftExtensionStatusBuilder()

    @property
    def name(self) -> str:
        return "minecraft"

    def initialize(self, context: GameExtensionContext) -> None:
        super().initialize(context)
        self._context = context
        if self.plugin is None:
            self._maybe_build_plugin()
        self._sync_runtime_context_resources()
        self._is_initialized = True

    def start(self) -> None:
        if self._is_started:
            return
        if not self._is_initialized:
            log_print("[MinecraftGameExtension] initialize must be called before start")
        self._ensure_plugin_ready()
        self._is_started = True
        self.mark_started(True)
        self.publish_event("extension_started")

    def stop(self) -> None:
        #20260725_kpopmodder: Lifecycle shutdown must not send an in-game stop command implicitly.
        self._is_started = False
        self.mark_started(False)

    def shutdown(self) -> None:
        self.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        command_dto = self.record_command(command)
        self._ensure_plugin_ready()
        dispatch_result = self.command_dispatcher.dispatch(
            self.plugin,
            command,
            command_dto,
        )
        return self._finalize_command_result(
            dispatch_result.result,
            action=dispatch_result.action,
        )

    def preview_command(self, command: Any) -> Dict[str, Any]:
        self._ensure_plugin_ready()
        preview_command = getattr(self.plugin, "preview_command", None)
        if not callable(preview_command):
            return {"ok": False, "error": "missing_plugin_preview"}
        result = preview_command(command)
        return dict(result) if isinstance(result, dict) else {"ok": False, "raw": result}

    def get_status(self) -> Dict[str, Any]:
        self._sync_runtime_context_resources()
        plugin_status = self.status_builder.build(
            name=self.name,
            plugin=self.plugin,
            runtime_context=getattr(self, "runtime_context", None),
            initialized=self._is_initialized,
            started=self._is_started,
        )
        return self.apply_status_contract(plugin_status)

    def _ensure_plugin_ready(self) -> None:
        if self.plugin is None:
            self._maybe_build_plugin()
        self._sync_runtime_context_resources()

    def _maybe_build_plugin(self) -> None:
        self.plugin = self.plugin_loader.build()

    def _finalize_command_result(
        self,
        result: Any,
        action: str = "",
    ) -> Dict[str, Any]:
        result_dto = self.record_result(result, action=action)
        return result_dto.to_legacy_dict()

    def _sync_runtime_context_resources(self) -> None:
        self.resource_syncer.sync(
            getattr(self, "runtime_context", None),
            self.plugin,
        )
