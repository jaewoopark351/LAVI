#20260905_kpopmodder: Assemble focused Fabric ChatClef handshake components.
from plugins.Minecraft.fabric.chatclef.transport.server.client_session.handshake.fabric_chatclef_handshake_acknowledger import FabricChatClefHandshakeAcknowledger
from plugins.Minecraft.fabric.chatclef.transport.server.client_session.handshake.fabric_chatclef_handshake_admission import FabricChatClefHandshakeAdmission
from plugins.Minecraft.fabric.chatclef.transport.server.client_session.handshake.fabric_chatclef_handshake_diagnostics import FabricChatClefHandshakeDiagnostics
from plugins.Minecraft.fabric.chatclef.transport.server.client_session.handshake.fabric_chatclef_session_id_factory import FabricChatClefSessionIdFactory
from plugins.Minecraft.fabric.chatclef.transport.server.client_session.handshake.fabric_chatclef_handshake_session_persistence import FabricChatClefHandshakeSessionPersistence


class FabricChatClefHandshakeComponentGraph:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        session_registry,
        diagnostics,
        envelope_transport,
        status_provider,
        now_ms,
    ) -> None:
        self.session_id_factory = FabricChatClefSessionIdFactory()
        self.admission = FabricChatClefHandshakeAdmission(
            connection_ownership=connection_ownership,
            command_lock=command_lock,
        )
        self.persistence = FabricChatClefHandshakeSessionPersistence(
            session_registry=session_registry,
            now_ms=now_ms,
        )
        self.acknowledger = FabricChatClefHandshakeAcknowledger(
            envelope_transport=envelope_transport,
            status_provider=status_provider,
        )
        self.diagnostics = FabricChatClefHandshakeDiagnostics(diagnostics)


__all__ = ("FabricChatClefHandshakeComponentGraph",)
