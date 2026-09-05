package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260905_kpopmodder: Decide only ordinary terminal-result retry eligibility and delay.

public final class FabricChatClefCommandTerminalRetryPolicy {
    private final FabricChatClefResultSendRetrySchedule retrySchedule =
            new FabricChatClefResultSendRetrySchedule();

    public boolean canRetry(
            FabricChatClefCommandResultSendOutcome outcome,
            int attemptCount
    ) {
        return outcome.retryable()
                && attemptCount < FabricChatClefResultSendRetrySchedule.MAX_SEND_ATTEMPTS;
    }

    public long delayMs(int attemptCount) {
        return retrySchedule.delayMs(attemptCount);
    }
}
