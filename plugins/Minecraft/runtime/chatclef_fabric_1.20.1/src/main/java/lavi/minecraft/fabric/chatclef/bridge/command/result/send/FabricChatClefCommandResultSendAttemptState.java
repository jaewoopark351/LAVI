package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260905_kpopmodder: Own only one ordinary terminal result's bounded send-attempt state.

import java.util.concurrent.atomic.AtomicBoolean;

public final class FabricChatClefCommandResultSendAttemptState {
    private final FabricChatClefCommandTerminalRetryPolicy retryPolicy;
    private final AtomicBoolean sendInFlight = new AtomicBoolean(false);
    private final AtomicBoolean sent = new AtomicBoolean(false);
    private volatile int sendAttemptCount;
    private volatile long nextSendAttemptAtMs;
    private volatile boolean sendRetryExhausted;
    private volatile String lastSendOutcome = "";

    public FabricChatClefCommandResultSendAttemptState(
            FabricChatClefCommandTerminalRetryPolicy retryPolicy
    ) {
        this.retryPolicy = retryPolicy;
    }

    public boolean begin(long nowMs) {
        if (sent.get() || sendRetryExhausted || nowMs < nextSendAttemptAtMs) {
            return false;
        }
        if (!sendInFlight.compareAndSet(false, true)) {
            return false;
        }
        sendAttemptCount++;
        lastSendOutcome = "attempt_" + sendAttemptCount + "_in_flight";
        return true;
    }

    public boolean complete(FabricChatClefCommandResultSendOutcome outcome) {
        FabricChatClefCommandResultSendOutcome sendOutcome = outcome == null
                ? FabricChatClefCommandResultSendOutcome.failed(
                        FabricChatClefCommandResultSendStatus.SEND_FAILED,
                        "missing send outcome"
                )
                : outcome;
        sendInFlight.set(false);
        lastSendOutcome = sendOutcome.diagnosticMessage();
        if (sendOutcome.succeeded()) {
            sendRetryExhausted = false;
            nextSendAttemptAtMs = 0L;
            return sent.compareAndSet(false, true);
        }
        if (!retryPolicy.canRetry(sendOutcome, sendAttemptCount)) {
            sendRetryExhausted = true;
            nextSendAttemptAtMs = Long.MAX_VALUE;
            return false;
        }
        nextSendAttemptAtMs = System.currentTimeMillis() + retryPolicy.delayMs(sendAttemptCount);
        return false;
    }

    public void cancel(String reason) {
        sendInFlight.set(false);
        lastSendOutcome = reason == null ? "" : reason;
    }

    public boolean sendInFlight() {
        return sendInFlight.get();
    }

    public boolean sent() {
        return sent.get();
    }

    public boolean retryExhausted() {
        return sendRetryExhausted;
    }

    public long nextAttemptAtMs() {
        return nextSendAttemptAtMs;
    }

    public int attemptCount() {
        return sendAttemptCount;
    }

    public String state() {
        if (sent.get()) {
            return "sent";
        }
        if (sendInFlight.get()) {
            return "in_flight";
        }
        if (sendRetryExhausted) {
            return "retry_exhausted";
        }
        if (nextSendAttemptAtMs > System.currentTimeMillis()) {
            return "backoff";
        }
        return "ready";
    }

    public boolean ready(long nowMs) {
        return !sent.get()
                && !sendInFlight.get()
                && !sendRetryExhausted
                && nowMs >= nextSendAttemptAtMs;
    }

    public String lastOutcome() {
        return lastSendOutcome;
    }
}
