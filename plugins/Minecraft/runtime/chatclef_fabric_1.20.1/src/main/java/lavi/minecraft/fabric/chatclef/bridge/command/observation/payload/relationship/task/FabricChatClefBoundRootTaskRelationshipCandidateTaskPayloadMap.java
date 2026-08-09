package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.task;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260809_kpopmodder: Keep candidate task snapshots separate without changing emitted keys.
public final class FabricChatClefBoundRootTaskRelationshipCandidateTaskPayloadMap {
    private FabricChatClefBoundRootTaskRelationshipCandidateTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String candidateName,
            FabricChatClefTaskSnapshot candidateTask
    ) {
        payload.put(candidateName, candidateTask.toMap());
    }
}
