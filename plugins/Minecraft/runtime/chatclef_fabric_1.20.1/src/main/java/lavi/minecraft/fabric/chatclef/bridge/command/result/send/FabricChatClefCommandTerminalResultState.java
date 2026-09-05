package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260905_kpopmodder: Preserve terminal-result state API over focused payload, send, and log-gate owners.

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.function.Supplier;

public final class FabricChatClefCommandTerminalResultState {
    private final FabricChatClefCommandTerminalPayloadCommitState payloadState =
            new FabricChatClefCommandTerminalPayloadCommitState();
    private final FabricChatClefCommandResultSendAttemptState sendState =
            new FabricChatClefCommandResultSendAttemptState(
                    new FabricChatClefCommandTerminalRetryPolicy()
            );
    private final FabricChatClefCommandTerminalDetachLogGate detachLogGate =
            new FabricChatClefCommandTerminalDetachLogGate();

    public FabricChatClefCommandResultPayload commitPayload(
            Supplier<FabricChatClefCommandResultPayload> factory
    ) {
        return payloadState.commit(factory);
    }

    public boolean payloadCommitted() {
        return payloadState.committed();
    }

    public boolean beginSend(long nowMs) {
        return sendState.begin(nowMs);
    }

    public boolean completeSend(FabricChatClefCommandResultSendOutcome outcome) {
        return sendState.complete(outcome);
    }

    public void cancelSendAttempt(String reason) {
        sendState.cancel(reason);
    }

    public boolean sendInFlight() {
        return sendState.sendInFlight();
    }

    public boolean sent() {
        return sendState.sent();
    }

    public boolean sendRetryExhausted() {
        return sendState.retryExhausted();
    }

    public long nextSendAttemptAtMs() {
        return sendState.nextAttemptAtMs();
    }

    public int sendAttemptCount() {
        return sendState.attemptCount();
    }

    public String sendState() {
        return sendState.state();
    }

    public boolean sendReady(long nowMs) {
        return sendState.ready(nowMs);
    }

    public String lastSendOutcome() {
        return sendState.lastOutcome();
    }

    public boolean markDetachDeferLogged() {
        return detachLogGate.markLogged();
    }
}
