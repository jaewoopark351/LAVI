package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission;

//20260905_kpopmodder: Preserve command-admission API over focused session and payload policies.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefCommandRequestAdmission {
    private final FabricChatClefCommandSessionAdmission sessionAdmission;
    private final FabricChatClefCommandPayloadAdmission payloadAdmission;

    public FabricChatClefCommandRequestAdmission(FabricChatClefSessionGuard sessionGuard) {
        this.sessionAdmission = new FabricChatClefCommandSessionAdmission(sessionGuard);
        this.payloadAdmission = new FabricChatClefCommandPayloadAdmission();
    }

    public FabricChatClefCommandRequestAdmissionDecision admitSession(
            FabricChatClefBridgeEnvelope envelope,
            long generation
    ) {
        return sessionAdmission.admit(envelope, generation);
    }

    public FabricChatClefCommandRequestAdmissionDecision admitPayload(
            FabricChatClefCommandRequest request
    ) {
        return payloadAdmission.admit(request);
    }
}
