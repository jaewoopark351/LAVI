package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Preserve whether a STOP send produced an immediate completion, including null.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

public record FabricChatClefStopControlResultSubmissionDecision(
        boolean immediateCompletion,
        FabricChatClefCommandResultSendOutcome outcome
) {
    public static FabricChatClefStopControlResultSubmissionDecision pending() {
        return new FabricChatClefStopControlResultSubmissionDecision(false, null);
    }

    public static FabricChatClefStopControlResultSubmissionDecision immediate(
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        return new FabricChatClefStopControlResultSubmissionDecision(true, outcome);
    }
}
