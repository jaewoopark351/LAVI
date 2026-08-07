package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefWaitingForTerminalConditionDetailsPayloadMap {
    private static final String WAITING_REASON = "waiting_reason";
    private static final String CURRENT_TASK_BOUND_ROOT_MATCH_REASON = "current_task_bound_root_match_reason";
    private static final String RUNTIME = "runtime";

    private FabricChatClefWaitingForTerminalConditionDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String waitingReason,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask,
            String currentTaskBoundRootMatchReason,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> details = new HashMap<>();
        details.put(WAITING_REASON, waitingReason);
        details.putAll(currentTask.toMap());
        details.put(CURRENT_TASK_BOUND_ROOT_MATCH_REASON, currentTaskBoundRootMatchReason);
        details.put(RUNTIME, runtime.toMap());
        return details;
    }
}
