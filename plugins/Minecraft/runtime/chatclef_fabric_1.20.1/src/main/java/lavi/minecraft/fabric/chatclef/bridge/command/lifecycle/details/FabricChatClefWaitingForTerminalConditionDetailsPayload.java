package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefWaitingForTerminalConditionDetailsPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

public final class FabricChatClefWaitingForTerminalConditionDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private final String waitingReason;
    private final FabricChatClefBoundRootTaskRelationshipPayload currentTask;
    private final String currentTaskBoundRootMatchReason;
    private final FabricChatClefTaskRuntimeObservationPayload runtime;

    public FabricChatClefWaitingForTerminalConditionDetailsPayload(
            String waitingReason,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask,
            String currentTaskBoundRootMatchReason,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        this.waitingReason = FabricChatClefLifecycleDetailValues.nullToEmpty(waitingReason);
        this.currentTask = currentTask;
        this.currentTaskBoundRootMatchReason = FabricChatClefLifecycleDetailValues.nullToEmpty(
                currentTaskBoundRootMatchReason
        );
        this.runtime = runtime;
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefWaitingForTerminalConditionDetailsPayloadMap.toMap(
                waitingReason,
                currentTask,
                currentTaskBoundRootMatchReason,
                runtime
        );
    }
}
