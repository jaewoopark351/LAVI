#20260905_kpopmodder: Compose the focused handlers for one Fabric ChatClef client session.
from __future__ import annotations

from .fabric_chatclef_active_message_dispatcher import (
    FabricChatClefActiveMessageDispatcher,
)
from .fabric_chatclef_client_disconnect_cleanup import (
    FabricChatClefClientDisconnectCleanup,
)
from .fabric_chatclef_client_session_coordinator import (
    FabricChatClefClientSessionCoordinator,
)
from .fabric_chatclef_handshake_handler import FabricChatClefHandshakeHandler
from .fabric_chatclef_inbound_envelope_reader import (
    FabricChatClefInboundEnvelopeReader,
)
from .handshake import FabricChatClefHandshakeComponentGraph


class FabricChatClefClientSessionComponentGraph:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        session_registry,
        diagnostics,
        envelope_transport,
        command_result_handler,
        stop_control_result_demultiplexer,
        status_provider,
        stopping_provider,
        last_error_reporter,
        now_ms,
    ) -> None:
        self.envelope_reader = FabricChatClefInboundEnvelopeReader(
            envelope_transport=envelope_transport
        )
        self.handshake_component_graph = FabricChatClefHandshakeComponentGraph(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            session_registry=session_registry,
            diagnostics=diagnostics,
            envelope_transport=envelope_transport,
            status_provider=status_provider,
            now_ms=now_ms,
        )
        self.handshake_handler = FabricChatClefHandshakeHandler(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            session_registry=session_registry,
            diagnostics=diagnostics,
            envelope_transport=envelope_transport,
            status_provider=status_provider,
            now_ms=now_ms,
            component_graph=self.handshake_component_graph,
        )
        self.active_message_dispatcher = FabricChatClefActiveMessageDispatcher(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            diagnostics=diagnostics,
            envelope_transport=envelope_transport,
            command_result_handler=command_result_handler,
            stop_control_result_demultiplexer=stop_control_result_demultiplexer,
            status_provider=status_provider,
        )
        self.disconnect_cleanup = FabricChatClefClientDisconnectCleanup(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
            session_registry=session_registry,
            diagnostics=diagnostics,
        )
        self.coordinator = FabricChatClefClientSessionCoordinator(
            envelope_reader=self.envelope_reader,
            handshake_handler=self.handshake_handler,
            active_message_dispatcher=self.active_message_dispatcher,
            disconnect_cleanup=self.disconnect_cleanup,
            diagnostics=diagnostics,
            stopping_provider=stopping_provider,
            last_error_reporter=last_error_reporter,
        )


__all__ = ("FabricChatClefClientSessionComponentGraph",)
