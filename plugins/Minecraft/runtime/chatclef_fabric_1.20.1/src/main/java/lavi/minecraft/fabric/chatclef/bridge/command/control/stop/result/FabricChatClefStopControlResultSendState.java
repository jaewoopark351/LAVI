package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Own only one STOP result's bounded send-attempt state.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

public final class FabricChatClefStopControlResultSendState {
    private final FabricChatClefStopControlResultRetryPolicy retryPolicy;
    private boolean sendInFlight;
    private boolean sent;
    private boolean quarantined;
    private int sendAttempts;
    private long nextAttemptAtMs;

    public FabricChatClefStopControlResultSendState(
            FabricChatClefStopControlResultRetryPolicy retryPolicy
    ) {
        this.retryPolicy = retryPolicy;
    }

    public synchronized boolean begin(long nowMs) {
        if (sent || quarantined || sendInFlight || nowMs < nextAttemptAtMs) {
            return false;
        }
        sendInFlight = true;
        sendAttempts++;
        return true;
    }

    public synchronized boolean complete(
            FabricChatClefCommandResultSendOutcome outcome,
            long nowMs
    ) {
        if (sent || quarantined) {
            return false;
        }
        sendInFlight = false;
        if (outcome != null && outcome.succeeded()) {
            sent = true;
            return true;
        }
        if (!retryPolicy.canRetry(outcome, sendAttempts)) {
            quarantined = true;
            nextAttemptAtMs = Long.MAX_VALUE;
            return false;
        }
        nextAttemptAtMs = nowMs + retryPolicy.delayMs(sendAttempts);
        return false;
    }

    public synchronized boolean ready(long nowMs) {
        return !sent && !quarantined && !sendInFlight && nowMs >= nextAttemptAtMs;
    }

    public synchronized boolean sent() {
        return sent;
    }

    public synchronized boolean quarantined() {
        return quarantined;
    }

    public synchronized int attempts() {
        return sendAttempts;
    }
}
