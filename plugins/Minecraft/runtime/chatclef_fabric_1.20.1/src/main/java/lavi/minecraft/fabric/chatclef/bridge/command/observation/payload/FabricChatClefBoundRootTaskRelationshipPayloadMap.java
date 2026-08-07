package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized bound-root relationship Map serialization without changing emitted fields.
public final class FabricChatClefBoundRootTaskRelationshipPayloadMap {
    private static final String MATCHES_BOUND_ROOT_TASK_SUFFIX = "_matches_bound_root_task";
    private static final String BOUND_ROOT_MATCH_REASON_SUFFIX = "_bound_root_match_reason";
    private static final String BOUND_ROOT_TASK = "bound_root_task";

    private FabricChatClefBoundRootTaskRelationshipPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String candidateName,
            FabricChatClefTaskSnapshot candidateTask,
            boolean matchesBoundRootTask,
            String boundRootMatchReason,
            FabricChatClefTaskSnapshot boundRootTask
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(candidateName, candidateTask.toMap());
        payload.put(candidateName + MATCHES_BOUND_ROOT_TASK_SUFFIX, matchesBoundRootTask);
        payload.put(candidateName + BOUND_ROOT_MATCH_REASON_SUFFIX, boundRootMatchReason);
        payload.put(BOUND_ROOT_TASK, boundRootTask.toMap());
        return payload;
    }
}
