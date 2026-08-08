package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.match.FabricChatClefTaskFinishedEventTaskRelationshipPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.match.FabricChatClefTaskFinishedMatchReasonPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.match.FabricChatClefTaskFinishedMatchedRootPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260808_kpopmodder: Keep task-finished bound-root match fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedMatchPayloadMap {
    private FabricChatClefTaskFinishedMatchPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason
    ) {
        FabricChatClefTaskFinishedMatchedRootPayloadMap.writeTo(payload, matchedBoundRootTask);
        FabricChatClefTaskFinishedEventTaskRelationshipPayloadMap.writeTo(payload, eventTaskRelationship);
        FabricChatClefTaskFinishedMatchReasonPayloadMap.writeTo(payload, eventTaskBoundRootMatchReason);
    }
}
