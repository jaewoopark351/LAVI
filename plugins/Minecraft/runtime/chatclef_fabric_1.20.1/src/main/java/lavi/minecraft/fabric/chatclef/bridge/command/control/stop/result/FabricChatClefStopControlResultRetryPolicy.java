package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Decide only same-connection STOP result retry eligibility and delay.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefResultSendRetrySchedule;

public final class FabricChatClefStopControlResultRetryPolicy {
    private final FabricChatClefResultSendRetrySchedule retrySchedule =
            new FabricChatClefResultSendRetrySchedule();

    public boolean canRetry(
            FabricChatClefCommandResultSendOutcome outcome,
            int attemptCount
    ) {
        FabricChatClefCommandResultSendStatus status = outcome == null ? null : outcome.status();
        boolean sameConnectionRetry = status == FabricChatClefCommandResultSendStatus.SEND_FAILED
                || status == FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED;
        return sameConnectionRetry
                && attemptCount < FabricChatClefResultSendRetrySchedule.MAX_SEND_ATTEMPTS;
    }

    public long delayMs(int attemptCount) {
        return retrySchedule.delayMs(attemptCount);
    }
}
