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
from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefNaturalLanguageService,
    ChatClefTranslationResultDTO,
)


class MinecraftFabricChatClefExtension(GameExtensionInterface):
    EXTENSION_NAME = "minecraft_fabric_chatclef"

    def __init__(
        self,
        plugin: Any = None,
        adapter: FabricChatClefAdapter | None = None,
        natural_language_service: ChatClefNaturalLanguageService | None = None,
    ):
        self.plugin = plugin
        self.adapter = adapter or self._adapter_from_plugin(plugin)
        self.natural_language_service = (
            natural_language_service or ChatClefNaturalLanguageService()
        )
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

    def translate_natural_language_command(self, command: Any) -> dict[str, Any]:
        text = self._natural_language_text(command)
        return self.natural_language_service.translate(text).to_dict()

    def handle_natural_language_command(self, command: Any) -> dict[str, Any]:
        text = self._natural_language_text(command)
        translation = self.natural_language_service.translate(text)
        if not translation.executable:
            payload = self._translation_rejection_payload(translation)
            self.record_result(payload, action="translate_natural_language_command")
            return payload
        return self.handle_command(
            self._translated_command_request(command, translation, text)
        )

    def submit_translated_command(
        self,
        command: Any,
        translation: Any,
    ) -> dict[str, Any]:
        text = self._natural_language_text(command)
        translated = ChatClefTranslationResultDTO.from_mapping(translation)
        if not translated.executable:
            payload = self._translation_rejection_payload(translated)
            self.record_result(payload, action="submit_translated_command")
            return payload
        return self.handle_command(
            self._translated_command_request(command, translated, text)
        )

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

    def _natural_language_text(self, command: Any) -> str:
        if isinstance(command, str):
            return command.strip()
        if isinstance(command, Mapping):
            payload = dict(command)
            return str(payload.get("text") or payload.get("command") or "").strip()
        return str(command or "").strip()

    def _translated_command_request(
        self,
        command: Any,
        translation: ChatClefTranslationResultDTO,
        original_text: str,
    ) -> CommandRequestDTO:
        payload = dict(command) if isinstance(command, Mapping) else {}
        metadata = dict(payload.get("metadata") or {})
        metadata["natural_language"] = {
            "language": "ko",
            "original_text": original_text,
            "translation": translation.to_dict(),
        }
        return CommandRequestDTO(
            request_id=payload.get("request_id", f"lavi-ko-{uuid.uuid4().hex}"),
            command=str(translation.command or ""),
            source=payload.get("source", "lavi_korean_intent"),
            deadline_ms=payload.get("deadline_ms"),
            metadata=metadata,
        )

    def _extension_result_payload(self, result: CommandResultDTO) -> dict[str, Any]:
        return {
            "ok": result.ok,
            "status": result.to_dict(),
            "error": None if result.error_code is None else result.error_code.value,
            "message": result.message,
            "details": result.data,
        }

    def _translation_rejection_payload(
        self,
        translation: ChatClefTranslationResultDTO,
    ) -> dict[str, Any]:
        return {
            "ok": False,
            "status": translation.to_dict(),
            "error": translation.reason_code,
            "message": translation.message,
            "details": translation.data,
        }

    def _plugin_status(self) -> dict[str, Any]:
        status = getattr(self.plugin, "get_status", None)
        if callable(status):
            return dict(status())
        return {"present": self.plugin is not None}
