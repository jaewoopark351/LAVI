package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission;

//20260905_kpopmodder: Carry one command_request admission decision and its accepted session identity.

import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

public final class FabricChatClefCommandRequestAdmissionDecision {
    private final FabricChatClefAcceptedSessionIdentity acceptedIdentity;
    private final String rejectionCode;
    private final String rejectionMessage;

    private FabricChatClefCommandRequestAdmissionDecision(
            FabricChatClefAcceptedSessionIdentity acceptedIdentity,
            String rejectionCode,
            String rejectionMessage
    ) {
        this.acceptedIdentity = acceptedIdentity;
        this.rejectionCode = rejectionCode;
        this.rejectionMessage = rejectionMessage;
    }

    public static FabricChatClefCommandRequestAdmissionDecision accepted(
            FabricChatClefAcceptedSessionIdentity acceptedIdentity
    ) {
        return new FabricChatClefCommandRequestAdmissionDecision(acceptedIdentity, "", "");
    }

    public static FabricChatClefCommandRequestAdmissionDecision rejected(
            String rejectionCode,
            String rejectionMessage
    ) {
        return new FabricChatClefCommandRequestAdmissionDecision(
                null,
                rejectionCode,
                rejectionMessage
        );
    }

    public boolean accepted() {
        return rejectionCode.isEmpty();
    }

    public FabricChatClefAcceptedSessionIdentity acceptedIdentity() {
        return acceptedIdentity;
    }

    public String rejectionCode() {
        return rejectionCode;
    }

    public String rejectionMessage() {
        return rejectionMessage;
    }
}
