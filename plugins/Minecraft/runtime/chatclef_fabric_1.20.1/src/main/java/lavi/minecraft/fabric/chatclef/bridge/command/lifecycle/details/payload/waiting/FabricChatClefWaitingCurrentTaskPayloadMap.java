package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260808_kpopmodder: Keep terminal-wait current-task fields separate without changing emitted keys.
public final class FabricChatClefWaitingCurrentTaskPayloadMap {
    private static final String CURRENT_TASK_BOUND_ROOT_MATCH_REASON = "current_task_bound_root_match_reason";

    private FabricChatClefWaitingCurrentTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> details,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask,
            String currentTaskBoundRootMatchReason
    ) {
        details.putAll(currentTask.toMap());
        details.put(CURRENT_TASK_BOUND_ROOT_MATCH_REASON, currentTaskBoundRootMatchReason);
    }
}
