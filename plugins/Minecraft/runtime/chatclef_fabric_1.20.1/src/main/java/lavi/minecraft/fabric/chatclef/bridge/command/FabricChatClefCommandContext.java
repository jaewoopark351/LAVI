package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.ownership.FabricChatClefCommandOwnershipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;

import java.util.concurrent.atomic.AtomicBoolean;

//20260801_kpopmodder: Bind one command to its Fabric websocket session, envelope, and connection generation.
public final class FabricChatClefCommandContext {
    private static final int MAX_TERMINAL_SEND_ATTEMPTS = 5;
    private static final long[] TERMINAL_SEND_RETRY_DELAYS_MS = {
            250L,
            500L,
            1000L,
            2000L,
            5000L
    };

    private final FabricChatClefCommandRequest request;
    private final String correlationId;
    private final String sessionId;
    private final long connectionGeneration;
    private final long acceptedAtMs;
    private final AtomicBoolean terminalSendInFlight = new AtomicBoolean(false);
    private final AtomicBoolean terminalSent = new AtomicBoolean(false);
    private volatile int terminalSendAttemptCount;
    private volatile long nextTerminalSendAttemptAtMs;
    private volatile boolean terminalSendRetryExhausted;
    private volatile String lastTerminalSendOutcome = "";
    private volatile boolean terminalDetachDeferLogged;
    private volatile boolean detached;
    private volatile String detachedReason = "";

    public FabricChatClefCommandContext(
            FabricChatClefCommandRequest request,
            String correlationId,
            String sessionId,
            long connectionGeneration
    ) {
        this.request = request;
        this.correlationId = nullToEmpty(correlationId);
        this.sessionId = nullToEmpty(sessionId);
        this.connectionGeneration = connectionGeneration;
        this.acceptedAtMs = System.currentTimeMillis();
    }

    public FabricChatClefCommandRequest request() {
        return request;
    }

    public String requestId() {
        return request == null ? "" : nullToEmpty(request.requestId);
    }

    public String correlationId() {
        return correlationId;
    }

    public String sessionId() {
        return sessionId;
    }

    public long connectionGeneration() {
        return connectionGeneration;
    }

    public boolean isDeadlineExceeded(long nowMs) {
        return request != null && request.isDeadlineExceeded(nowMs);
    }

    public boolean beginTerminalSend() {
        return beginTerminalSend(System.currentTimeMillis());
    }

    public boolean beginTerminalSend(long nowMs) {
        if (terminalSent.get()) {
            return false;
        }
        if (terminalSendRetryExhausted || nowMs < nextTerminalSendAttemptAtMs) {
            return false;
        }
        if (!terminalSendInFlight.compareAndSet(false, true)) {
            return false;
        }
        terminalSendAttemptCount++;
        lastTerminalSendOutcome = "attempt_" + terminalSendAttemptCount + "_in_flight";
        return true;
    }

    public boolean completeTerminalSend(FabricChatClefCommandResultSendOutcome outcome) {
        FabricChatClefCommandResultSendOutcome sendOutcome = outcome == null
                ? FabricChatClefCommandResultSendOutcome.failed(
                        FabricChatClefCommandResultSendStatus.SEND_FAILED,
                        "missing send outcome"
                )
                : outcome;
        terminalSendInFlight.set(false);
        lastTerminalSendOutcome = sendOutcome.diagnosticMessage();
        if (sendOutcome.succeeded()) {
            terminalSendRetryExhausted = false;
            nextTerminalSendAttemptAtMs = 0L;
            return terminalSent.compareAndSet(false, true);
        }
        if (!sendOutcome.retryable() || terminalSendAttemptCount >= MAX_TERMINAL_SEND_ATTEMPTS) {
            terminalSendRetryExhausted = true;
            nextTerminalSendAttemptAtMs = Long.MAX_VALUE;
            return false;
        }
        nextTerminalSendAttemptAtMs = System.currentTimeMillis() + terminalRetryDelayMs(terminalSendAttemptCount);
        return false;
    }

    public boolean completeTerminalSend(boolean succeeded) {
        if (succeeded) {
            return completeTerminalSend(FabricChatClefCommandResultSendOutcome.sent());
        }
        return completeTerminalSend(
                FabricChatClefCommandResultSendOutcome.failed(
                        FabricChatClefCommandResultSendStatus.SEND_FAILED,
                        "terminal send failed"
                )
        );
    }

    public void cancelTerminalSendAttempt(String reason) {
        terminalSendInFlight.set(false);
        lastTerminalSendOutcome = nullToEmpty(reason);
    }

    public boolean terminalSendInFlight() {
        return terminalSendInFlight.get();
    }

    public boolean terminalSent() {
        return terminalSent.get();
    }

    public boolean terminalSendRetryExhausted() {
        return terminalSendRetryExhausted;
    }

    public long nextTerminalSendAttemptAtMs() {
        return nextTerminalSendAttemptAtMs;
    }

    public int terminalSendAttemptCount() {
        return terminalSendAttemptCount;
    }

    public String terminalSendState() {
        if (terminalSent.get()) {
            return "sent";
        }
        if (terminalSendInFlight.get()) {
            return "in_flight";
        }
        if (terminalSendRetryExhausted) {
            return "retry_exhausted";
        }
        if (nextTerminalSendAttemptAtMs > System.currentTimeMillis()) {
            return "backoff";
        }
        return "ready";
    }

    public boolean terminalSendReady(long nowMs) {
        return !terminalSent.get()
                && !terminalSendInFlight.get()
                && !terminalSendRetryExhausted
                && nowMs >= nextTerminalSendAttemptAtMs;
    }

    public String lastTerminalSendOutcome() {
        return lastTerminalSendOutcome;
    }

    public boolean markTerminalDetachDeferLogged() {
        if (terminalDetachDeferLogged) {
            return false;
        }
        terminalDetachDeferLogged = true;
        return true;
    }

    private static long terminalRetryDelayMs(int attemptCount) {
        int index = Math.max(0, Math.min(attemptCount - 1, TERMINAL_SEND_RETRY_DELAYS_MS.length - 1));
        return TERMINAL_SEND_RETRY_DELAYS_MS[index];
    }

    public void markDetached(String reason) {
        detached = true;
        detachedReason = nullToEmpty(reason);
    }

    public FabricChatClefCommandOwnershipPayload ownershipPayload() {
        return FabricChatClefCommandOwnershipPayload.of(
                requestId(),
                correlationId,
                sessionId,
                connectionGeneration,
                acceptedAtMs,
                detached,
                detachedReason
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
