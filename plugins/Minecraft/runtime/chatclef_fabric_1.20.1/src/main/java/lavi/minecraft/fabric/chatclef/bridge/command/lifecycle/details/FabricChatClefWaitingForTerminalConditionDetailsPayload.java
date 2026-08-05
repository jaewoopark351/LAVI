package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefWaitingForTerminalConditionDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private static final String WAITING_REASON = "waiting_reason";
    private static final String CURRENT_TASK_BOUND_ROOT_MATCH_REASON = "current_task_bound_root_match_reason";
    private static final String RUNTIME = "runtime";

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
        Map<String, Object> details = new HashMap<>();
        details.put(WAITING_REASON, waitingReason);
        details.putAll(currentTask.toMap());
        details.put(CURRENT_TASK_BOUND_ROOT_MATCH_REASON, currentTaskBoundRootMatchReason);
        details.put(RUNTIME, runtime.toMap());
        return details;
    }
}
