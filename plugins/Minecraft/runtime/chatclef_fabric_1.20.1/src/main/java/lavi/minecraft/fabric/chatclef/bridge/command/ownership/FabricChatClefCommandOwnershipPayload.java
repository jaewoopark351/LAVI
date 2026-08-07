package lavi.minecraft.fabric.chatclef.bridge.command.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.FabricChatClefCommandOwnershipPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.Map;

//20260805_kpopmodder: Isolate command ownership diagnostic payload keys without changing their map shape.
public final class FabricChatClefCommandOwnershipPayload implements FabricChatClefCommandResultDataPayload {
    private final String requestId;
    private final String correlationId;
    private final String sessionId;
    private final long connectionGeneration;
    private final long acceptedAtMs;
    private final boolean detached;
    private final String detachedReason;

    private FabricChatClefCommandOwnershipPayload(
            String requestId,
            String correlationId,
            String sessionId,
            long connectionGeneration,
            long acceptedAtMs,
            boolean detached,
            String detachedReason
    ) {
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.sessionId = sessionId;
        this.connectionGeneration = connectionGeneration;
        this.acceptedAtMs = acceptedAtMs;
        this.detached = detached;
        this.detachedReason = detachedReason;
    }

    public static FabricChatClefCommandOwnershipPayload of(
            String requestId,
            String correlationId,
            String sessionId,
            long connectionGeneration,
            long acceptedAtMs,
            boolean detached,
            String detachedReason
    ) {
        return new FabricChatClefCommandOwnershipPayload(
                requestId,
                correlationId,
                sessionId,
                connectionGeneration,
                acceptedAtMs,
                detached,
                detachedReason
        );
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefCommandOwnershipPayloadMap.toMap(
                requestId,
                correlationId,
                sessionId,
                connectionGeneration,
                acceptedAtMs,
                detached,
                detachedReason
        );
    }
}
