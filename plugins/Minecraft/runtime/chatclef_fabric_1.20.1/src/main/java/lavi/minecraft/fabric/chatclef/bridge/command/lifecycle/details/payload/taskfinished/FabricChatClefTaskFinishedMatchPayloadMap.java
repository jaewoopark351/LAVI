package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260808_kpopmodder: Keep task-finished bound-root match fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedMatchPayloadMap {
    private static final String MATCHED_BOUND_ROOT_TASK = "matched_bound_root_task";
    private static final String EVENT_TASK_BOUND_ROOT_MATCH_REASON = "event_task_bound_root_match_reason";

    private FabricChatClefTaskFinishedMatchPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason
    ) {
        payload.put(MATCHED_BOUND_ROOT_TASK, matchedBoundRootTask);
        payload.putAll(eventTaskRelationship.toMap());
        payload.put(EVENT_TASK_BOUND_ROOT_MATCH_REASON, eventTaskBoundRootMatchReason);
    }
}
