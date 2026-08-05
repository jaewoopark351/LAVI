package lavi.minecraft.fabric.chatclef.bridge.command.observation;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Keep bound-root task relationship diagnostic fields typed until the Map edge.
public final class FabricChatClefBoundRootTaskRelationshipPayload {
    private static final String MATCHES_BOUND_ROOT_TASK_SUFFIX = "_matches_bound_root_task";
    private static final String BOUND_ROOT_MATCH_REASON_SUFFIX = "_bound_root_match_reason";
    private static final String BOUND_ROOT_TASK = "bound_root_task";

    private final String candidateName;
    private final FabricChatClefTaskSnapshot candidateTask;
    private final boolean matchesBoundRootTask;
    private final String boundRootMatchReason;
    private final FabricChatClefTaskSnapshot boundRootTask;

    private FabricChatClefBoundRootTaskRelationshipPayload(
            String candidateName,
            FabricChatClefTaskSnapshot candidateTask,
            boolean matchesBoundRootTask,
            String boundRootMatchReason,
            FabricChatClefTaskSnapshot boundRootTask
    ) {
        this.candidateName = candidateName;
        this.candidateTask = candidateTask;
        this.matchesBoundRootTask = matchesBoundRootTask;
        this.boundRootMatchReason = boundRootMatchReason;
        this.boundRootTask = boundRootTask;
    }

    public static FabricChatClefBoundRootTaskRelationshipPayload of(
            String candidateName,
            FabricChatClefTaskSnapshot candidateTask,
            boolean matchesBoundRootTask,
            String boundRootMatchReason,
            FabricChatClefTaskSnapshot boundRootTask
    ) {
        return new FabricChatClefBoundRootTaskRelationshipPayload(
                candidateName,
                candidateTask,
                matchesBoundRootTask,
                boundRootMatchReason,
                boundRootTask
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put(candidateName, candidateTask.toMap());
        payload.put(candidateName + MATCHES_BOUND_ROOT_TASK_SUFFIX, matchesBoundRootTask);
        payload.put(candidateName + BOUND_ROOT_MATCH_REASON_SUFFIX, boundRootMatchReason);
        payload.put(BOUND_ROOT_TASK, boundRootTask.toMap());
        return payload;
    }
}
