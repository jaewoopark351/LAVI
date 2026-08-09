package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.match;

import java.util.Map;

//20260809_kpopmodder: Keep bound-root relationship match reasons separate without changing emitted keys.
public final class FabricChatClefBoundRootTaskRelationshipMatchReasonPayloadMap {
    private static final String BOUND_ROOT_MATCH_REASON_SUFFIX = "_bound_root_match_reason";

    private FabricChatClefBoundRootTaskRelationshipMatchReasonPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String candidateName,
            String boundRootMatchReason
    ) {
        payload.put(candidateName + BOUND_ROOT_MATCH_REASON_SUFFIX, boundRootMatchReason);
    }
}
