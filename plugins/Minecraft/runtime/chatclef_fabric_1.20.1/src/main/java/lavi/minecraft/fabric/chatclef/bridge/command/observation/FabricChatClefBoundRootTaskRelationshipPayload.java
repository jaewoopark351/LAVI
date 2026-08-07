package lavi.minecraft.fabric.chatclef.bridge.command.observation;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.FabricChatClefBoundRootTaskRelationshipPayloadMap;

import java.util.Map;

//20260805_kpopmodder: Keep bound-root task relationship diagnostic fields typed until the Map edge.
public final class FabricChatClefBoundRootTaskRelationshipPayload {
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
        return FabricChatClefBoundRootTaskRelationshipPayloadMap.toMap(
                candidateName,
                candidateTask,
                matchesBoundRootTask,
                boundRootMatchReason,
                boundRootTask
        );
    }
}
