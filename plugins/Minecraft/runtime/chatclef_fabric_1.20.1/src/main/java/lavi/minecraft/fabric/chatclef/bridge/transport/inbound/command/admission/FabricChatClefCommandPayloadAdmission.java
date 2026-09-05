package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission;

//20260905_kpopmodder: Decide only whether an ordinary command request has its required payload.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;

public final class FabricChatClefCommandPayloadAdmission {
    public FabricChatClefCommandRequestAdmissionDecision admit(
            FabricChatClefCommandRequest request
    ) {
        if (request.isValid()) {
            return FabricChatClefCommandRequestAdmissionDecision.accepted(null);
        }
        return FabricChatClefCommandRequestAdmissionDecision.rejected(
                "invalid_request",
                "Fabric ChatClef command_request requires request_id and command."
        );
    }
}
