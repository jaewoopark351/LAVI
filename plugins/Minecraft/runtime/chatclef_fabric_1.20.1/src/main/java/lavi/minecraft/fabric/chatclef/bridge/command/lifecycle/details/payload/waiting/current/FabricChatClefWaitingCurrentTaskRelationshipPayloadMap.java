package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting.current;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260809_kpopmodder: Keep terminal-wait current-task relationship fields separate without changing emitted keys.
public final class FabricChatClefWaitingCurrentTaskRelationshipPayloadMap {
    private FabricChatClefWaitingCurrentTaskRelationshipPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> details,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask
    ) {
        details.putAll(currentTask.toMap());
    }
}
