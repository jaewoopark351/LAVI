package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.FabricChatClefBoundRootTaskRelationshipMatchPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.FabricChatClefBoundRootTaskRelationshipTaskPayloadMap;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized bound-root relationship Map serialization without changing emitted fields.
public final class FabricChatClefBoundRootTaskRelationshipPayloadMap {
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
        FabricChatClefBoundRootTaskRelationshipTaskPayloadMap.writeTo(
                payload,
                candidateName,
                candidateTask,
                boundRootTask
        );
        FabricChatClefBoundRootTaskRelationshipMatchPayloadMap.writeTo(
                payload,
                candidateName,
                matchesBoundRootTask,
                boundRootMatchReason
        );
        return payload;
    }
}
