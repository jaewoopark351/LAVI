package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission;

//20260905_kpopmodder: Decide only ordinary command admission against the accepted session identity.

import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefCommandSessionAdmission {
    private final FabricChatClefSessionGuard sessionGuard;

    public FabricChatClefCommandSessionAdmission(FabricChatClefSessionGuard sessionGuard) {
        this.sessionGuard = sessionGuard;
    }

    public FabricChatClefCommandRequestAdmissionDecision admit(
            FabricChatClefBridgeEnvelope envelope,
            long generation
    ) {
        if (!sessionGuard.handshakeAccepted()) {
            return FabricChatClefCommandRequestAdmissionDecision.rejected(
                    "not_connected",
                    "Fabric ChatClef bridge handshake has not been accepted."
            );
        }
        if (!sessionGuard.isActiveSession(envelope.sessionId)) {
            return FabricChatClefCommandRequestAdmissionDecision.rejected(
                    "invalid_request",
                    "Fabric ChatClef command_request session does not match active handshake."
            );
        }
        FabricChatClefAcceptedSessionIdentity acceptedIdentity =
                sessionGuard.acceptedIdentity().orElse(null);
        if (acceptedIdentity == null || acceptedIdentity.javaSocketGeneration() != generation) {
            return FabricChatClefCommandRequestAdmissionDecision.rejected(
                    "not_connected",
                    "Fabric ChatClef command_request is not bound to this WebSocket generation."
            );
        }
        return FabricChatClefCommandRequestAdmissionDecision.accepted(acceptedIdentity);
    }
}
