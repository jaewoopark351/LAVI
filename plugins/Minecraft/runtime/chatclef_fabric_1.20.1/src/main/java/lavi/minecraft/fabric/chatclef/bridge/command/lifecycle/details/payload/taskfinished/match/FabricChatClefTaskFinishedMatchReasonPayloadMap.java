package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.match;

import java.util.Map;

//20260809_kpopmodder: Keep task-finished match-reason fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedMatchReasonPayloadMap {
    private static final String EVENT_TASK_BOUND_ROOT_MATCH_REASON = "event_task_bound_root_match_reason";

    private FabricChatClefTaskFinishedMatchReasonPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String eventTaskBoundRootMatchReason) {
        payload.put(EVENT_TASK_BOUND_ROOT_MATCH_REASON, eventTaskBoundRootMatchReason);
    }
}
