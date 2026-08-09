package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.match.FabricChatClefBoundRootTaskRelationshipMatchFlagPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.match.FabricChatClefBoundRootTaskRelationshipMatchReasonPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Isolate bound-root relationship match fields at the Map edge.
public final class FabricChatClefBoundRootTaskRelationshipMatchPayloadMap {
    private FabricChatClefBoundRootTaskRelationshipMatchPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String candidateName,
            boolean matchesBoundRootTask,
            String boundRootMatchReason
    ) {
        FabricChatClefBoundRootTaskRelationshipMatchFlagPayloadMap.writeTo(
                payload,
                candidateName,
                matchesBoundRootTask
        );
        FabricChatClefBoundRootTaskRelationshipMatchReasonPayloadMap.writeTo(
                payload,
                candidateName,
                boundRootMatchReason
        );
    }
}
