#20260905_kpopmodder: Preserve the websocket server API as a focused delegation facade.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.fabric.chatclef.config.fabric_chatclef_config import (
    FabricChatClefConfig,
)
from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.input.stop import StopControlClaimRegistry
from plugins.Minecraft.fabric.chatclef.session import FabricChatClefSessionRegistry

from ..fabric_chatclef_websocket_server_factory import (
    serve_fabric_chatclef_websocket,
)
from .runtime import FabricChatClefServerComponentGraph, FabricChatClefServerLifecycle
from .runtime.fabric_chatclef_server_runtime_values import (
    fabric_chatclef_now_ms,
    new_fabric_chatclef_message_id,
)


class FabricChatClefWebSocketServer:
    def __init__(
        self,
        config: FabricChatClefConfig,
        session_registry: FabricChatClefSessionRegistry,
        diagnostics: FabricChatClefDiagnostics,
    ) -> None:
        self._config = config
        self._session_registry = session_registry
        self._diagnostics = diagnostics
        self._lifecycle = FabricChatClefServerLifecycle(
            config=config,
            diagnostics=diagnostics,
            websocket_server_factory=serve_fabric_chatclef_websocket,
        )
        self._components = FabricChatClefServerComponentGraph(
            config=config,
            session_registry=session_registry,
            diagnostics=diagnostics,
            lifecycle=self._lifecycle,
            message_id_factory=self._new_message_id,
            now_ms=self._now_ms,
        )
        self._command_lock = self._components.command_lock
        self._connection_ownership = self._components.connection_ownership
        self._status_builder = self._components.status_builder
        self._client_handler = self._components.client_handler
        self._command_submitter = self._components.command_submitter
        self._crafting_feedback_api = self._components.crafting_feedback_api
        self._command_feedback_api = self._components.command_feedback_api
        self._stop_control_claim_registry = (
            self._components.stop_control_claim_registry
        )
        self._stop_control_admission_barrier = (
            self._components.stop_control_admission_barrier
        )
        self._stop_control_tracker_registry = (
            self._components.stop_control_tracker_registry
        )
        self._stop_control_terminal_listener = (
            self._components.stop_control_terminal_listener
        )
        self._stop_control_submitter = self._components.stop_control_submitter

    @property
    def endpoint(self) -> str:
        return self._lifecycle.endpoint

    @property
    def last_error(self) -> str | None:
        return self._lifecycle.last_error

    @property
    def is_running(self) -> bool:
        return self._lifecycle.is_running

    def start(self) -> None:
        self._lifecycle.start()

    def stop(self) -> None:
        self._lifecycle.stop()

    def status_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        return self.local_status_snapshot(enabled=enabled)

    def local_status_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        return self._components.status_facade.local_snapshot(enabled=enabled)

    def wire_status_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        return self._components.status_facade.wire_snapshot(enabled=enabled)

    def submit_command(self, request: CommandRequestDTO) -> CommandResultDTO:
        return self._components.command_api.submit(request)

    def submit_stop_control(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ):
        return self._components.stop_api.submit(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )

    def get_stop_control_claim_registry(self) -> StopControlClaimRegistry:
        return self._components.stop_api.claim_registry()

    def set_stop_terminal_response_callback(self, callback) -> None:
        self._components.stop_api.set_terminal_response_callback(callback)

    #20260907_kpopmodder: Keep crafting lifecycle access behind its command-locked API.
    def reserve_crafting_feedback(self, grant: object) -> bool:
        return self.reserve_command_feedback(grant)

    def abandon_crafting_feedback(self, grant: object) -> bool:
        return self.abandon_command_feedback(grant)

    def claim_crafting_feedback_start(
        self,
        grant: object,
        result: object,
    ):
        return self.claim_command_feedback_start(grant, result)

    def inspect_crafting_feedback_status(self, target_item: str | None):
        return self._crafting_feedback_api.inspect_status(target_item)

    def set_crafting_terminal_response_callback(self, callback) -> None:
        self.set_command_lifecycle_terminal_response_callback(callback)

    def reserve_command_feedback(self, grant: object) -> bool:
        return self._command_feedback_api.reserve(grant)

    def abandon_command_feedback(self, grant: object) -> bool:
        return self._command_feedback_api.abandon(grant)

    def claim_command_feedback_start(self, grant: object, result: object):
        return self._command_feedback_api.claim_start(grant, result)

    def inspect_command_feedback_status(self, query: object):
        return self._command_feedback_api.inspect_status(query)

    def set_command_lifecycle_terminal_response_callback(self, callback) -> None:
        self._command_feedback_api.set_terminal_callback(callback)

    def set_command_terminal_response_callback(self, callback) -> None:
        self.set_command_lifecycle_terminal_response_callback(callback)

    def _record_client_error(self, message: str) -> None:
        self._lifecycle.record_client_error(message)

    @staticmethod
    def _new_message_id() -> str:
        return new_fabric_chatclef_message_id()

    @staticmethod
    def _now_ms() -> int:
        return fabric_chatclef_now_ms()
