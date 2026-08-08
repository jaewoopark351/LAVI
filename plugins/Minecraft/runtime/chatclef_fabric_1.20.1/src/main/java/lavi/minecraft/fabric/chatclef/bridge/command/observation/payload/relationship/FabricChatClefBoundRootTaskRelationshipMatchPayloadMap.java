package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship;

import java.util.Map;

//20260808_kpopmodder: Isolate bound-root relationship match fields at the Map edge.
public final class FabricChatClefBoundRootTaskRelationshipMatchPayloadMap {
    private static final String MATCHES_BOUND_ROOT_TASK_SUFFIX = "_matches_bound_root_task";
    private static final String BOUND_ROOT_MATCH_REASON_SUFFIX = "_bound_root_match_reason";

    private FabricChatClefBoundRootTaskRelationshipMatchPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String candidateName,
            boolean matchesBoundRootTask,
            String boundRootMatchReason
    ) {
        payload.put(candidateName + MATCHES_BOUND_ROOT_TASK_SUFFIX, matchesBoundRootTask);
        payload.put(candidateName + BOUND_ROOT_MATCH_REASON_SUFFIX, boundRootMatchReason);
    }
}
