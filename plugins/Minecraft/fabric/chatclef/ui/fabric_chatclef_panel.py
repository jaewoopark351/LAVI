#20260801_kpopmodder: Own only the Fabric ChatClef Gradio panel and UI callbacks.
from __future__ import annotations

import json
import uuid
from typing import Any, Mapping

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)


class FabricChatClefPanel:
    TAB_LABEL = "Minecraft Fabric"

    def __init__(self, plugin: Any, extension: Any = None):
        self.plugin = plugin
        self.extension = extension

    def create_ui(self) -> None:
        import gradio as gr

        initial_status = self.status_payload()
        endpoint, lifecycle, connected, status_json = self.status_values(
            initial_status,
        )

        with gr.Tab(self.TAB_LABEL):
            with gr.Row():
                endpoint_box = gr.Textbox(
                    label="Endpoint",
                    value=endpoint,
                    lines=1,
                    interactive=False,
                )
                lifecycle_box = gr.Textbox(
                    label="Lifecycle",
                    value=lifecycle,
                    lines=1,
                    interactive=False,
                )
                connected_box = gr.Textbox(
                    label="Connected",
                    value=connected,
                    lines=1,
                    interactive=False,
                )
            command_box = gr.Textbox(
                label="ChatClef Command",
                value="",
                lines=1,
            )
            with gr.Row():
                send_button = gr.Button("Send")
                refresh_button = gr.Button("Refresh")
            result_box = gr.Textbox(
                label="Command Result",
                value="",
                lines=8,
                interactive=False,
            )
            status_box = gr.Textbox(
                label="Status",
                value=status_json,
                lines=14,
                interactive=False,
            )

            refresh_outputs = [
                endpoint_box,
                lifecycle_box,
                connected_box,
                status_box,
            ]
            refresh_button.click(
                self.on_refresh_click,
                inputs=[],
                outputs=refresh_outputs,
            )
            send_button.click(
                self.on_submit_command_click,
                inputs=[command_box],
                outputs=[result_box, *refresh_outputs],
            )
            command_box.submit(
                self.on_submit_command_click,
                inputs=[command_box],
                outputs=[result_box, *refresh_outputs],
            )

    def on_refresh_click(self) -> tuple[str, str, str, str]:
        return self.status_values(self.status_payload())

    def on_submit_command_click(self, command: Any) -> tuple[str, str, str, str, str]:
        command_text = str(command or "").strip()
        if not command_text:
            result = self.empty_command_result()
        else:
            result = self.submit_command(command_text)
        endpoint, lifecycle, connected, status_json = self.status_values(
            self.status_payload(),
        )
        return self.to_json(result), endpoint, lifecycle, connected, status_json

    def status_payload(self) -> dict[str, Any]:
        provider = None
        if self.extension is not None:
            provider = getattr(self.extension, "get_status", None)
        if not callable(provider):
            provider = getattr(self.plugin, "get_status", None)
        if not callable(provider):
            return {
                "error": "status_provider_missing",
                "details": "Fabric ChatClef status provider is unavailable.",
            }
        try:
            return self.mapping_payload(provider())
        except Exception as error:
            return {
                "error": "status_provider_failed",
                "details": f"{type(error).__name__}: {error}",
            }

    def status_values(self, status: Mapping[str, Any]) -> tuple[str, str, str, str]:
        bridge = self.bridge_status(status)
        endpoint = self.endpoint_text(status, bridge)
        lifecycle = str(bridge.get("lifecycle_state") or "unknown")
        connected = "true" if bool(bridge.get("connected")) else "false"
        return endpoint, lifecycle, connected, self.to_json(status)

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
                return self.mapping_payload(handler(request))
            except Exception as error:
                return self.exception_result(request["request_id"], error)
        return self.submit_with_plugin_adapter(CommandRequestDTO.from_mapping(request))

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

    def bridge_status(self, status: Mapping[str, Any]) -> dict[str, Any]:
        payload = self.mapping_payload(status)
        details = self.mapping_payload(payload.get("details"))
        if "lifecycle_state" in details:
            return details
        bridge = self.mapping_payload(payload.get("bridge"))
        if "lifecycle_state" in bridge:
            return bridge
        plugin = self.mapping_payload(payload.get("plugin"))
        plugin_bridge = self.mapping_payload(plugin.get("bridge"))
        if "lifecycle_state" in plugin_bridge:
            return plugin_bridge
        return payload

    def endpoint_text(
        self,
        status: Mapping[str, Any],
        bridge: Mapping[str, Any],
    ) -> str:
        bridge_details = self.mapping_payload(bridge.get("details"))
        endpoint = bridge_details.get("endpoint") or status.get("endpoint")
        if endpoint:
            return str(endpoint)
        config = getattr(self.plugin, "config", None)
        return str(getattr(config, "endpoint", ""))

    def empty_command_result(self) -> dict[str, Any]:
        return self.error_result(
            "",
            BridgeErrorCode.INVALID_REQUEST,
            "ChatClef command is empty.",
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

    def mapping_payload(self, value: Any) -> dict[str, Any]:
        return dict(value) if isinstance(value, Mapping) else {}

    def to_json(self, value: Any) -> str:
        return json.dumps(
            value,
            ensure_ascii=False,
            indent=2,
            default=str,
        )
