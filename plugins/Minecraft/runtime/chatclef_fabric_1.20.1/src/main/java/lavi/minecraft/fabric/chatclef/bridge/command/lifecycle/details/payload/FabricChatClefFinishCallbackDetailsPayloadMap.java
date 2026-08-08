package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish.FabricChatClefFinishCallbackCurrentTaskPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish.FabricChatClefFinishCallbackRuntimePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefFinishCallbackDetailsPayloadMap {
    private FabricChatClefFinishCallbackDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> details = new HashMap<>();
        FabricChatClefFinishCallbackCurrentTaskPayloadMap.writeTo(details, callbackCurrentTask);
        FabricChatClefFinishCallbackRuntimePayloadMap.writeTo(details, runtime);
        return details;
    }
}
