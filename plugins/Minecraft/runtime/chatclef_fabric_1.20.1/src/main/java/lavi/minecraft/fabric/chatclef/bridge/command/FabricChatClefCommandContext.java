package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.ownership.FabricChatClefCommandOwnershipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandTerminalResultState;

import java.util.function.Supplier;

//20260801_kpopmodder: Bind one command to its Fabric websocket session, envelope, and connection generation.
public final class FabricChatClefCommandContext {
    private final FabricChatClefCommandRequest request;
    private final String correlationId;
    private final String sessionId;
    private final long serverConnectionGeneration;
    private final long javaSocketGeneration;
    private final long acceptedAtMs;
    private final FabricChatClefCommandTerminalResultState terminalResultState =
            new FabricChatClefCommandTerminalResultState();
    private volatile boolean detached;
    private volatile String detachedReason = "";

    public FabricChatClefCommandContext(
            FabricChatClefCommandRequest request,
            String correlationId,
            String sessionId,
            long connectionGeneration
    ) {
        this(request, correlationId, sessionId, connectionGeneration, connectionGeneration);
    }

    //20260905_kpopmodder: Keep wire server generation separate from the local WebSocket fence.
    public FabricChatClefCommandContext(
            FabricChatClefCommandRequest request,
            String correlationId,
            String sessionId,
            long serverConnectionGeneration,
            long javaSocketGeneration
    ) {
        this.request = request;
        this.correlationId = nullToEmpty(correlationId);
        this.sessionId = nullToEmpty(sessionId);
        this.serverConnectionGeneration = serverConnectionGeneration;
        this.javaSocketGeneration = javaSocketGeneration;
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
        return javaSocketGeneration;
    }

    public long serverConnectionGeneration() {
        return serverConnectionGeneration;
    }

    public boolean isDeadlineExceeded(long nowMs) {
        return request != null && request.isDeadlineExceeded(nowMs);
    }

    public boolean beginTerminalSend() {
        return beginTerminalSend(System.currentTimeMillis());
    }

    public FabricChatClefCommandResultPayload commitTerminalPayload(
            Supplier<FabricChatClefCommandResultPayload> factory
    ) {
        return terminalResultState.commitPayload(factory);
    }

    //20260905_kpopmodder: Let STOP refuse to replace an ordinary terminal classification already committed.
    public boolean terminalPayloadCommitted() {
        return terminalResultState.payloadCommitted();
    }

    public boolean beginTerminalSend(long nowMs) {
        return terminalResultState.beginSend(nowMs);
    }

    public boolean completeTerminalSend(FabricChatClefCommandResultSendOutcome outcome) {
        return terminalResultState.completeSend(outcome);
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
        terminalResultState.cancelSendAttempt(reason);
    }

    public boolean terminalSendInFlight() {
        return terminalResultState.sendInFlight();
    }

    public boolean terminalSent() {
        return terminalResultState.sent();
    }

    public boolean terminalSendRetryExhausted() {
        return terminalResultState.sendRetryExhausted();
    }

    public long nextTerminalSendAttemptAtMs() {
        return terminalResultState.nextSendAttemptAtMs();
    }

    public int terminalSendAttemptCount() {
        return terminalResultState.sendAttemptCount();
    }

    public String terminalSendState() {
        return terminalResultState.sendState();
    }

    public boolean terminalSendReady(long nowMs) {
        return terminalResultState.sendReady(nowMs);
    }

    public String lastTerminalSendOutcome() {
        return terminalResultState.lastSendOutcome();
    }

    public boolean markTerminalDetachDeferLogged() {
        return terminalResultState.markDetachDeferLogged();
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
                serverConnectionGeneration,
                acceptedAtMs,
                detached,
                detachedReason
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
