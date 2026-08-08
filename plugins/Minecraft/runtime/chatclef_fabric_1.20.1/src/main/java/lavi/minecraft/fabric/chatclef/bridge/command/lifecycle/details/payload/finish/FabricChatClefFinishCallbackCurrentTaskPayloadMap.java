package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish.current.FabricChatClefFinishCallbackCurrentTaskRelationshipPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260808_kpopmodder: Keep finish-callback current-task fields separate without changing emitted keys.
public final class FabricChatClefFinishCallbackCurrentTaskPayloadMap {
    private FabricChatClefFinishCallbackCurrentTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> details,
            FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask
    ) {
        FabricChatClefFinishCallbackCurrentTaskRelationshipPayloadMap.writeTo(details, callbackCurrentTask);
    }
}
