package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish.current;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260809_kpopmodder: Keep finish-callback current-task relationship fields separate without changing emitted keys.
public final class FabricChatClefFinishCallbackCurrentTaskRelationshipPayloadMap {
    private FabricChatClefFinishCallbackCurrentTaskRelationshipPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> details,
            FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask
    ) {
        details.putAll(callbackCurrentTask.toMap());
    }
}
