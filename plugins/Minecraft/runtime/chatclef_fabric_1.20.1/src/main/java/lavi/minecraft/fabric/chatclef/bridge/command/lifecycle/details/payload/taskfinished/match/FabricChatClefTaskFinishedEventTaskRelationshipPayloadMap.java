package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.match;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260809_kpopmodder: Keep task-finished event-task relationship fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedEventTaskRelationshipPayloadMap {
    private FabricChatClefTaskFinishedEventTaskRelationshipPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship
    ) {
        payload.putAll(eventTaskRelationship.toMap());
    }
}
