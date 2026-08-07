package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

public final class FabricChatClefFinishCallbackDetailsPayloadMap {
    private static final String RUNTIME = "runtime";

    private FabricChatClefFinishCallbackDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> details = callbackCurrentTask.toMap();
        details.put(RUNTIME, runtime.toMap());
        return details;
    }
}
