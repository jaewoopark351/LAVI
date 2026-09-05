package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage;

//20260905_kpopmodder: Represent the one-shot registered STOP invocation outcome.

public record FabricChatClefStopControlInvocationOutcome(boolean succeeded, String failureReason) {
    public static FabricChatClefStopControlInvocationOutcome success() {
        return new FabricChatClefStopControlInvocationOutcome(true, "");
    }

    public static FabricChatClefStopControlInvocationOutcome failed(String reason) {
        return new FabricChatClefStopControlInvocationOutcome(false, reason);
    }
}
