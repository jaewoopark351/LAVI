package lavi.minecraft.fabric.chatclef.bridge.transport.result;

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

//20260905_kpopmodder: Represent command-result transport admission without performing submission.
public final class FabricChatClefResultSendAdmissionDecision {
    private final boolean admitted;
    private final FabricChatClefCommandResultSendSubmission rejection;

    private FabricChatClefResultSendAdmissionDecision(
            boolean admitted,
            FabricChatClefCommandResultSendSubmission rejection
    ) {
        this.admitted = admitted;
        this.rejection = rejection;
    }

    public static FabricChatClefResultSendAdmissionDecision admitted() {
        return new FabricChatClefResultSendAdmissionDecision(true, null);
    }

    public static FabricChatClefResultSendAdmissionDecision rejected(
            FabricChatClefCommandResultSendSubmission rejection
    ) {
        return new FabricChatClefResultSendAdmissionDecision(false, rejection);
    }

    public boolean isAdmitted() {
        return admitted;
    }

    public FabricChatClefCommandResultSendSubmission rejection() {
        return rejection;
    }
}
