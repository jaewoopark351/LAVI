package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.match;

import java.util.Map;

//20260809_kpopmodder: Keep bound-root relationship match flags separate without changing emitted keys.
public final class FabricChatClefBoundRootTaskRelationshipMatchFlagPayloadMap {
    private static final String MATCHES_BOUND_ROOT_TASK_SUFFIX = "_matches_bound_root_task";

    private FabricChatClefBoundRootTaskRelationshipMatchFlagPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String candidateName,
            boolean matchesBoundRootTask
    ) {
        payload.put(candidateName + MATCHES_BOUND_ROOT_TASK_SUFFIX, matchesBoundRootTask);
    }
}
