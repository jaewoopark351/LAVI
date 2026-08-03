#20260801_kpopmodder: Own only the Fabric ChatClef Gradio panel and UI callbacks.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.ui.fabric_chatclef_command_controller import (
    FabricChatClefCommandController,
)
from plugins.Minecraft.fabric.chatclef.ui.fabric_chatclef_json_formatter import (
    FabricChatClefJsonFormatter,
)
from plugins.Minecraft.fabric.chatclef.ui.fabric_chatclef_status_presenter import (
    FabricChatClefStatusPresenter,
)


class FabricChatClefPanel:
    TAB_LABEL = "Minecraft Fabric"
    KOREAN_COMMAND_LABEL = "Korean Minecraft Command"

    def __init__(self, plugin: Any, extension: Any = None):
        self.plugin = plugin
        self.extension = extension
        self.formatter = FabricChatClefJsonFormatter()
        self.status_presenter = FabricChatClefStatusPresenter(
            plugin=plugin,
            extension=extension,
            formatter=self.formatter,
        )
        self.command_controller = FabricChatClefCommandController(
            plugin=plugin,
            extension=extension,
            status_presenter=self.status_presenter,
            formatter=self.formatter,
        )

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
            korean_command_box = gr.Textbox(
                label=self.KOREAN_COMMAND_LABEL,
                value="",
                lines=1,
            )
            with gr.Row():
                send_button = gr.Button("Send")
                send_korean_button = gr.Button("Send Korean")
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
            send_korean_button.click(
                self.on_submit_korean_command_click,
                inputs=[korean_command_box],
                outputs=[result_box, *refresh_outputs],
            )
            korean_command_box.submit(
                self.on_submit_korean_command_click,
                inputs=[korean_command_box],
                outputs=[result_box, *refresh_outputs],
            )

    def on_refresh_click(self) -> tuple[str, str, str, str]:
        return self.status_values(self.status_payload())

    def on_submit_command_click(self, command: Any) -> tuple[str, str, str, str, str]:
        return self.command_controller.on_submit_command_click(command)

    def on_submit_korean_command_click(
        self,
        command: Any,
    ) -> tuple[str, str, str, str, str]:
        return self.command_controller.on_submit_korean_command_click(command)

    def status_payload(self) -> dict[str, Any]:
        return self.status_presenter.status_payload()

    def status_values(self, status: dict[str, Any]) -> tuple[str, str, str, str]:
        return self.status_presenter.status_values(status)

    def submit_command(self, command_text: str) -> dict[str, Any]:
        return self.command_controller.submit_command(command_text)

    def submit_korean_command(self, command_text: str) -> dict[str, Any]:
        return self.command_controller.submit_korean_command(command_text)

    def empty_command_result(self) -> dict[str, Any]:
        return self.command_controller.empty_command_result()

    def mapping_payload(self, value: Any) -> dict[str, Any]:
        return self.formatter.mapping_payload(value)

    def to_json(self, value: Any) -> str:
        return self.formatter.to_json(value)

    def bridge_status(self, status: dict[str, Any]) -> dict[str, Any]:
        return self.status_presenter.bridge_status(status)

    def endpoint_text(
        self,
        status: dict[str, Any],
        bridge: dict[str, Any],
    ) -> str:
        return self.status_presenter.endpoint_text(status, bridge)

    def submit_with_plugin_adapter(self, request: Any) -> dict[str, Any]:
        return self.command_controller.submit_with_plugin_adapter(request)

    def exception_result(self, request_id: str, error: Exception) -> dict[str, Any]:
        return self.command_controller.exception_result(request_id, error)

    def error_result(self, request_id: str, error_code: Any, message: str) -> dict[str, Any]:
        return self.command_controller.error_result(
            request_id,
            error_code,
            message,
        )
