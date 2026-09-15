# 20260915_kpopmodder: Real lifecycle ownership and result graph with a recording transport, no game or socket I/O.
from types import SimpleNamespace
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.session.fabric_chatclef_session_registry import (
    FabricChatClefSessionRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.server.runtime.fabric_chatclef_server_component_graph import (
    FabricChatClefServerComponentGraph,
)
from .recording_adapter_fixture import ConfirmationRecordingAdapter


class ConfirmationLifecycleAdapter(ConfirmationRecordingAdapter):
    def __init__(self):
        super().__init__()
        self.logs = []
        self.graph = FabricChatClefServerComponentGraph(
            config=SimpleNamespace(
                reconcile_stale_deposit_to_unknown_enabled=False,
                startup_timeout_sec=1.0,
            ),
            session_registry=FabricChatClefSessionRegistry(),
            diagnostics=SimpleNamespace(
                info=self.logs.append, warning=self.logs.append
            ),
            lifecycle=SimpleNamespace(
                stopping=False,
                loop=None,
                record_client_error=lambda _: None,
                bind=lambda **_: None,
            ),
            message_id_factory=lambda: "confirmation-message",
            now_ms=lambda: 1,
        )
        self.websocket = object()
        assert self.graph.connection_ownership.try_activate(
            websocket=self.websocket, session_id=self.session
        ).accepted

    def get_status(self):
        status = super().get_status()
        status.details["commands"].update(
            self.graph.connection_ownership.local_admission_snapshot()
        )
        return status

    def reserve_command_feedback(self, grant):
        return self.graph.command_feedback_api.reserve(grant)

    def abandon_command_feedback(self, grant):
        return self.graph.command_feedback_api.abandon(grant)

    def claim_command_feedback_start(self, grant, result):
        return self.graph.command_feedback_api.claim_start(grant, result)

    def set_command_lifecycle_terminal_response_callback(self, callback):
        self.graph.command_feedback_api.set_terminal_callback(callback)

    def submit_command(self, request):
        self.requests.append(request)
        with self.graph.command_lock:
            self.active = self.graph.connection_ownership.begin_command(
                request_id=request.request_id,
                command_message_id="confirmed-message",
                command=request.command,
                source=request.source,
                metadata=request.metadata,
            )
        assert self.active is not None
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
            data={
                "command": request.command,
                "session_id": self.active.session_id,
                "connection_generation": self.active.generation,
                "command_message_id": self.active.command_message_id,
            },
        )
