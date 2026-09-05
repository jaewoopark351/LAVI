package lavi.minecraft.fabric.chatclef.bridge.transport.session.validation;

//20260905_kpopmodder: Carry a pure handshake ACK validation result without mutating session state.

import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

public final class FabricChatClefHandshakeAckValidation {
    private final FabricChatClefAcceptedSessionIdentity identity;
    private final String rejectionCode;

    private FabricChatClefHandshakeAckValidation(
            FabricChatClefAcceptedSessionIdentity identity,
            String rejectionCode
    ) {
        this.identity = identity;
        this.rejectionCode = rejectionCode;
    }

    public static FabricChatClefHandshakeAckValidation accepted(
            FabricChatClefAcceptedSessionIdentity identity
    ) {
        return new FabricChatClefHandshakeAckValidation(identity, "");
    }

    public static FabricChatClefHandshakeAckValidation rejected(String rejectionCode) {
        return new FabricChatClefHandshakeAckValidation(null, rejectionCode);
    }

    public boolean accepted() {
        return identity != null;
    }

    public FabricChatClefAcceptedSessionIdentity identity() {
        return identity;
    }

    public String rejectionCode() {
        return rejectionCode;
    }
}
