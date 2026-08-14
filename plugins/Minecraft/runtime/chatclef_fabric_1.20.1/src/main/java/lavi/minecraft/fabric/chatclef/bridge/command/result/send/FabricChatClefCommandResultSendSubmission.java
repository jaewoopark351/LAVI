package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260815_kpopmodder: Separate WebSocket send scheduling from asynchronous send completion.
public final class FabricChatClefCommandResultSendSubmission {
    private final boolean accepted;
    private final FabricChatClefCommandResultSendOutcome immediateOutcome;

    private FabricChatClefCommandResultSendSubmission(
            boolean accepted,
            FabricChatClefCommandResultSendOutcome immediateOutcome
    ) {
        this.accepted = accepted;
        this.immediateOutcome = immediateOutcome;
    }

    public static FabricChatClefCommandResultSendSubmission accepted() {
        return new FabricChatClefCommandResultSendSubmission(true, FabricChatClefCommandResultSendOutcome.inFlight());
    }

    public static FabricChatClefCommandResultSendSubmission failed(FabricChatClefCommandResultSendOutcome outcome) {
        return new FabricChatClefCommandResultSendSubmission(false, outcome);
    }

    public boolean acceptedForAsyncSend() {
        return accepted;
    }

    public FabricChatClefCommandResultSendOutcome immediateOutcome() {
        return immediateOutcome;
    }

    public String diagnosticMessage() {
        return immediateOutcome.diagnosticMessage();
    }
}
