package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Carry one fully validated STOP control request as immutable scalar values.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;

public final class FabricChatClefStopControlRequest {
    private final FabricChatClefStopControlBaseRequest base;
    private final long deadlineMs;
    private final FabricChatClefStopControlTargetScope targetScope;
    private final String targetRequestId;
    private final String targetCommandMessageId;
    private final String targetSessionId;
    private final Long targetServerConnectionGeneration;

    public FabricChatClefStopControlRequest(
            FabricChatClefStopControlBaseRequest base,
            long deadlineMs,
            FabricChatClefStopControlTargetScope targetScope,
            String targetRequestId,
            String targetCommandMessageId,
            String targetSessionId,
            Long targetServerConnectionGeneration
    ) {
        this.base = base;
        this.deadlineMs = deadlineMs;
        this.targetScope = targetScope;
        this.targetRequestId = targetRequestId;
        this.targetCommandMessageId = targetCommandMessageId;
        this.targetSessionId = targetSessionId;
        this.targetServerConnectionGeneration = targetServerConnectionGeneration;
    }

    public FabricChatClefStopControlBaseRequest base() {
        return base;
    }

    public FabricChatClefStopControlIdentity identity() {
        return base.identity();
    }

    public long javaSocketGeneration() {
        return base.javaSocketGeneration();
    }

    public long deadlineMs() {
        return deadlineMs;
    }

    public FabricChatClefStopControlTargetScope targetScope() {
        return targetScope;
    }

    public String targetRequestId() {
        return targetRequestId;
    }

    public String targetCommandMessageId() {
        return targetCommandMessageId;
    }

    public String targetSessionId() {
        return targetSessionId;
    }

    public Long targetServerConnectionGeneration() {
        return targetServerConnectionGeneration;
    }

    public boolean deadlineExceeded(long nowMs) {
        return nowMs > deadlineMs;
    }

    public boolean matchesTarget(FabricChatClefCommandContext context) {
        return context != null
                && targetScope == FabricChatClefStopControlTargetScope.TRACKED_COMMAND
                && targetRequestId.equals(context.requestId())
                && targetCommandMessageId.equals(context.correlationId())
                && targetSessionId.equals(context.sessionId())
                && targetServerConnectionGeneration != null
                && targetServerConnectionGeneration == context.serverConnectionGeneration();
    }
}
