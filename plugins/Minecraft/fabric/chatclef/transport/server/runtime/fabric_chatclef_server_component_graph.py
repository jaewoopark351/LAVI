#20260905_kpopmodder: Compose the focused Fabric ChatClef websocket server collaborators.
from __future__ import annotations

import threading

from plugins.Minecraft.fabric.chatclef.input.stop import StopControlClaimRegistry

from ...command_submission import (
    FabricChatClefCommandResultHandler,
    FabricChatClefCommandSubmitter,
)
from ...control.stop import (
    StopControlAdmissionBarrier,
    StopControlResultDemultiplexer,
    StopControlSubmitter,
    StopControlTerminalListener,
    StopControlTrackerRegistry,
)
from ...fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from ..fabric_chatclef_client_handler import FabricChatClefClientHandler
from ..fabric_chatclef_envelope_transport import FabricChatClefEnvelopeTransport
from ..fabric_chatclef_status_snapshot_builder import (
    FabricChatClefStatusSnapshotBuilder,
)
from .fabric_chatclef_server_command_api import FabricChatClefServerCommandApi
from .fabric_chatclef_server_shutdown_state_reset import (
    FabricChatClefServerShutdownStateReset,
)
from .fabric_chatclef_server_status_facade import FabricChatClefServerStatusFacade
from .fabric_chatclef_server_stop_api import FabricChatClefServerStopApi


class FabricChatClefServerComponentGraph:
    def __init__(
        self,
        *,
        config,
        session_registry,
        diagnostics,
        lifecycle,
        message_id_factory,
        now_ms,
    ) -> None:
        self.command_lock = threading.RLock()
        self.connection_ownership = FabricChatClefConnectionOwnership(
            reconcile_stale_deposit_to_unknown_requested=(
                config.reconcile_stale_deposit_to_unknown_enabled
            ),
        )
        self.envelope_transport = FabricChatClefEnvelopeTransport(
            message_id_factory=message_id_factory,
            now_ms=now_ms,
        )
        self.command_result_handler = FabricChatClefCommandResultHandler(
            connection_ownership=self.connection_ownership,
            command_lock=self.command_lock,
            diagnostics=diagnostics,
        )
        self.stop_control_claim_registry = StopControlClaimRegistry()
        self.stop_control_admission_barrier = StopControlAdmissionBarrier()
        self.stop_control_tracker_registry = StopControlTrackerRegistry()
        self.stop_control_terminal_listener = StopControlTerminalListener()
        self.stop_control_result_demultiplexer = StopControlResultDemultiplexer(
            command_lock=self.command_lock,
            connection_ownership=self.connection_ownership,
            tracker_registry=self.stop_control_tracker_registry,
            admission_barrier=self.stop_control_admission_barrier,
            terminal_listener=self.stop_control_terminal_listener,
            diagnostics=diagnostics,
        )
        self.status_builder = FabricChatClefStatusSnapshotBuilder(
            connection_ownership=self.connection_ownership,
            command_lock=self.command_lock,
            session_registry=session_registry,
        )
        self.status_facade = FabricChatClefServerStatusFacade(
            status_builder=self.status_builder,
            lifecycle=lifecycle,
        )
        self.client_handler = FabricChatClefClientHandler(
            connection_ownership=self.connection_ownership,
            command_lock=self.command_lock,
            session_registry=session_registry,
            diagnostics=diagnostics,
            envelope_transport=self.envelope_transport,
            command_result_handler=self.command_result_handler,
            stop_control_result_demultiplexer=(
                self.stop_control_result_demultiplexer
            ),
            status_provider=lambda: self.status_facade.wire_snapshot(
                enabled=True
            ).to_dict(),
            stopping_provider=lambda: lifecycle.stopping,
            last_error_reporter=lifecycle.record_client_error,
            now_ms=now_ms,
        )
        self.stop_control_submitter = StopControlSubmitter(
            connection_ownership=self.connection_ownership,
            command_lock=self.command_lock,
            session_registry=session_registry,
            claim_registry=self.stop_control_claim_registry,
            admission_barrier=self.stop_control_admission_barrier,
            tracker_registry=self.stop_control_tracker_registry,
            diagnostics=diagnostics,
            loop_provider=lambda: lifecycle.loop,
            envelope_transport=self.envelope_transport,
            send_timeout_sec=config.startup_timeout_sec,
            message_id_factory=message_id_factory,
            now_ms=now_ms,
        )
        self.command_submitter = FabricChatClefCommandSubmitter(
            connection_ownership=self.connection_ownership,
            command_lock=self.command_lock,
            diagnostics=diagnostics,
            loop_provider=lambda: lifecycle.loop,
            envelope_transport=self.envelope_transport,
            send_timeout_sec=config.startup_timeout_sec,
            message_id_factory=message_id_factory,
            now_ms=now_ms,
            stop_control_admission_barrier=self.stop_control_admission_barrier,
        )
        self.shutdown_state_reset = FabricChatClefServerShutdownStateReset(
            connection_ownership=self.connection_ownership,
            command_lock=self.command_lock,
            session_registry=session_registry,
            stop_control_claim_registry=self.stop_control_claim_registry,
            stop_control_tracker_registry=self.stop_control_tracker_registry,
            stop_control_admission_barrier=self.stop_control_admission_barrier,
        )
        self.command_api = FabricChatClefServerCommandApi(
            command_submitter=self.command_submitter
        )
        self.stop_api = FabricChatClefServerStopApi(
            stop_control_submitter=self.stop_control_submitter,
            stop_control_claim_registry=self.stop_control_claim_registry,
            stop_control_terminal_listener=self.stop_control_terminal_listener,
        )
        lifecycle.bind(
            client_handler=self.client_handler,
            shutdown_state_reset=self.shutdown_state_reset,
        )
