package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.ownership.FabricChatClefCommandOwnershipPayload;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

//20260801_kpopmodder: Bind one command to its Fabric websocket session, envelope, and connection generation.
public final class FabricChatClefCommandContext {
    private final FabricChatClefCommandRequest request;
    private final String correlationId;
    private final String sessionId;
    private final long connectionGeneration;
    private final long acceptedAtMs;
    private final AtomicBoolean terminalSent = new AtomicBoolean(false);
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

    public boolean markTerminalSent() {
        return terminalSent.compareAndSet(false, true);
    }

    public void markDetached(String reason) {
        detached = true;
        detachedReason = nullToEmpty(reason);
    }

    public Map<String, Object> ownershipData() {
        return FabricChatClefCommandOwnershipPayload.of(
                requestId(),
                correlationId,
                sessionId,
                connectionGeneration,
                acceptedAtMs,
                detached,
                detachedReason
        ).toMap();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
