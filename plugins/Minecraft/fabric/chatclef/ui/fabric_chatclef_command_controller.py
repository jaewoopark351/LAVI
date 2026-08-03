#20260803_kpopmodder: Added Fabric ChatClef UI command submission outside the panel.
from __future__ import annotations

import uuid
from typing import Any

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.ui.fabric_chatclef_json_formatter import (
    FabricChatClefJsonFormatter,
)
from plugins.Minecraft.fabric.chatclef.ui.fabric_chatclef_status_presenter import (
    FabricChatClefStatusPresenter,
)


class FabricChatClefCommandController:
    def __init__(
        self,
        plugin: Any,
        extension: Any = None,
        status_presenter: FabricChatClefStatusPresenter | None = None,
        formatter: FabricChatClefJsonFormatter | None = None,
    ):
        self.plugin = plugin
        self.extension = extension
        self.formatter = formatter or FabricChatClefJsonFormatter()
        self.status_presenter = status_presenter or FabricChatClefStatusPresenter(
            plugin=plugin,
            extension=extension,
            formatter=self.formatter,
        )

    def on_submit_command_click(self, command: Any) -> tuple[str, str, str, str, str]:
        command_text = str(command or "").strip()
        if not command_text:
            result = self.empty_command_result()
        else:
            result = self.submit_command(command_text)
        return self._result_with_status(result)

    def on_submit_korean_command_click(
        self,
        command: Any,
    ) -> tuple[str, str, str, str, str]:
        command_text = str(command or "").strip()
        if not command_text:
            result = self.empty_korean_command_result()
        else:
            result = self.submit_korean_command(command_text)
        return self._result_with_status(result)

    def submit_command(self, command_text: str) -> dict[str, Any]:
        request = {
            "request_id": f"lavi-gui-{uuid.uuid4().hex}",
            "command": command_text,
            "source": "lavi_gui",
            "metadata": {"ui": "fabric_chatclef"},
        }
        handler = None
        if self.extension is not None:
            handler = getattr(self.extension, "handle_command", None)
        if callable(handler):
            try:
                return self.formatter.mapping_payload(handler(request))
            except Exception as error:
                return self.exception_result(request["request_id"], error)
        return self.submit_with_plugin_adapter(CommandRequestDTO.from_mapping(request))

    def submit_korean_command(self, command_text: str) -> dict[str, Any]:
        request = {
            "request_id": f"lavi-ko-gui-{uuid.uuid4().hex}",
            "text": command_text,
            "source": "lavi_gui_korean",
            "metadata": {"ui": "fabric_chatclef", "language": "ko"},
        }
        handler = None
        if self.extension is not None:
            handler = getattr(self.extension, "handle_natural_language_command", None)
        if not callable(handler):
            return self.error_result(
                request["request_id"],
                BridgeErrorCode.NOT_IMPLEMENTED,
                "Fabric ChatClef Korean command handler is unavailable.",
            )
        try:
            return self.formatter.mapping_payload(handler(request))
        except Exception as error:
            return self.exception_result(request["request_id"], error)

    def submit_with_plugin_adapter(
        self,
        request: CommandRequestDTO,
    ) -> dict[str, Any]:
        adapter_factory = getattr(self.plugin, "create_adapter", None)
        if not callable(adapter_factory):
            return self.error_result(
                request.request_id,
                BridgeErrorCode.NOT_IMPLEMENTED,
                "Fabric ChatClef adapter is unavailable.",
            )
        try:
            result = adapter_factory().submit_command(request)
            result_dto = CommandResultDTO.from_mapping(result)
            return {
                "ok": result_dto.ok,
                "status": result_dto.to_dict(),
                "error": (
                    None
                    if result_dto.error_code is None
                    else result_dto.error_code.value
                ),
                "message": result_dto.message,
                "details": result_dto.data,
            }
        except Exception as error:
            return self.exception_result(request.request_id, error)

    def empty_command_result(self) -> dict[str, Any]:
        return self.error_result(
            "",
            BridgeErrorCode.INVALID_REQUEST,
            "ChatClef command is empty.",
        )

    def empty_korean_command_result(self) -> dict[str, Any]:
        return self.error_result(
            "",
            BridgeErrorCode.INVALID_REQUEST,
            "Korean Minecraft command is empty.",
        )

    def exception_result(self, request_id: str, error: Exception) -> dict[str, Any]:
        return self.error_result(
            request_id,
            BridgeErrorCode.INTERNAL_ERROR,
            f"{type(error).__name__}: {error}",
        )

    def error_result(
        self,
        request_id: str,
        error_code: BridgeErrorCode,
        message: str,
    ) -> dict[str, Any]:
        result = CommandResultDTO(
            request_id=request_id,
            ok=False,
            status=CommandResultStatus.REJECTED,
            error_code=error_code,
            message=message,
            data={},
        )
        return {
            "ok": False,
            "status": result.to_dict(),
            "error": error_code.value,
            "message": message,
            "details": {},
        }

    def _result_with_status(
        self,
        result: dict[str, Any],
    ) -> tuple[str, str, str, str, str]:
        endpoint, lifecycle, connected, status_json = (
            self.status_presenter.status_values(self.status_presenter.status_payload())
        )
        return (
            self.formatter.to_json(result),
            endpoint,
            lifecycle,
            connected,
            status_json,
        )
