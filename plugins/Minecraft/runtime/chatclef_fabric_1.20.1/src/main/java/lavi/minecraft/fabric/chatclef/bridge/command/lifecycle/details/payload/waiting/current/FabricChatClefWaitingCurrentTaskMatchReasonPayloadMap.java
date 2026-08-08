package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting.current;

import java.util.Map;

//20260809_kpopmodder: Keep terminal-wait current-task match-reason fields separate without changing emitted keys.
public final class FabricChatClefWaitingCurrentTaskMatchReasonPayloadMap {
    private static final String CURRENT_TASK_BOUND_ROOT_MATCH_REASON = "current_task_bound_root_match_reason";

    private FabricChatClefWaitingCurrentTaskMatchReasonPayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, String currentTaskBoundRootMatchReason) {
        details.put(CURRENT_TASK_BOUND_ROOT_MATCH_REASON, currentTaskBoundRootMatchReason);
    }
}
