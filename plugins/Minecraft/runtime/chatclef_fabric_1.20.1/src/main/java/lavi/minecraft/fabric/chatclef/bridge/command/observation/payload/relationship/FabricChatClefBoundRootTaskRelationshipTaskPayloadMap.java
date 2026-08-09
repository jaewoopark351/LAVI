package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.task.FabricChatClefBoundRootTaskRelationshipBoundRootTaskPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.task.FabricChatClefBoundRootTaskRelationshipCandidateTaskPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Split bound-root relationship task snapshots without changing emitted keys.
public final class FabricChatClefBoundRootTaskRelationshipTaskPayloadMap {
    private FabricChatClefBoundRootTaskRelationshipTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String candidateName,
            FabricChatClefTaskSnapshot candidateTask,
            FabricChatClefTaskSnapshot boundRootTask
    ) {
        FabricChatClefBoundRootTaskRelationshipCandidateTaskPayloadMap.writeTo(
                payload,
                candidateName,
                candidateTask
        );
        FabricChatClefBoundRootTaskRelationshipBoundRootTaskPayloadMap.writeTo(
                payload,
                boundRootTask
        );
    }
}
