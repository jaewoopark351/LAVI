#20260801_kpopmodder: Register Fabric ChatClef as a LAVI game extension.
from __future__ import annotations

import uuid
from typing import Any, Mapping

from app_core.extensions.game_extension_interface import GameExtensionInterface
from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)


class MinecraftFabricChatClefExtension(GameExtensionInterface):
    EXTENSION_NAME = "minecraft_fabric_chatclef"

    def __init__(
        self,
        plugin: Any = None,
        adapter: FabricChatClefAdapter | None = None,
    ):
        self.plugin = plugin
        self.adapter = adapter or self._adapter_from_plugin(plugin)
        self.context = None
        self.runtime_context = None
        self.event_bus = None

    @property
    def name(self) -> str:
        return self.EXTENSION_NAME

    def start(self) -> None:
        self.adapter.start()
        status = self.adapter.get_status()
        self.mark_started(status.enabled and not bool(status.last_error_message))
        self.publish_event(
            "minecraft_fabric_chatclef_started",
            {"status": status.to_dict()},
        )

    def stop(self) -> None:
        self.adapter.stop()
        self.mark_started(False)
        self.publish_event("minecraft_fabric_chatclef_stopped", {})

    def handle_command(self, command: Any) -> dict[str, Any]:
        request = self._command_request(command)
        self.record_command(request.to_dict())
        result = self.adapter.submit_command(request)
        payload = self._extension_result_payload(result)
        self.record_result(payload, action="submit_command")
        return payload

    def get_status(self) -> dict[str, Any]:
        status = self.adapter.get_status().to_dict()
        return self.apply_status_contract(
            {
                "name": self.name,
                "plugin": self._plugin_status(),
                "runtime": {"backend_id": self.adapter.backend_id},
                "details": status,
                "error": status.get("last_error_message"),
            }
        )

    def _adapter_from_plugin(self, plugin: Any) -> FabricChatClefAdapter:
        adapter_factory = getattr(plugin, "create_adapter", None)
        if callable(adapter_factory):
            return adapter_factory()
        return FabricChatClefAdapter()

    def _command_request(self, command: Any) -> CommandRequestDTO:
        if isinstance(command, CommandRequestDTO):
            return command
        if isinstance(command, str):
            return CommandRequestDTO(
                request_id=f"lavi-command-{uuid.uuid4().hex}",
                command=command,
                source="lavi",
                metadata={},
            )
        if isinstance(command, Mapping):
            payload = dict(command)
            if "command" not in payload and "action" in payload:
                payload["command"] = payload["action"]
            payload.setdefault("request_id", f"lavi-command-{uuid.uuid4().hex}")
            payload.setdefault("source", "lavi")
            payload.setdefault("metadata", {})
            return CommandRequestDTO.from_mapping(payload)
        return CommandRequestDTO(
            request_id=f"lavi-command-{uuid.uuid4().hex}",
            command="",
            source="lavi",
            metadata={"raw_type": command.__class__.__name__},
        )

    def _extension_result_payload(self, result: CommandResultDTO) -> dict[str, Any]:
        return {
            "ok": result.ok,
            "status": result.to_dict(),
            "error": None if result.error_code is None else result.error_code.value,
            "message": result.message,
            "details": result.data,
        }

    def _plugin_status(self) -> dict[str, Any]:
        status = getattr(self.plugin, "get_status", None)
        if callable(status):
            return dict(status())
        return {"present": self.plugin is not None}
