package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

public final class FabricChatClefFinishCallbackDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private static final String RUNTIME = "runtime";

    private final FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask;
    private final FabricChatClefTaskRuntimeObservationPayload runtime;

    public FabricChatClefFinishCallbackDetailsPayload(
            FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        this.callbackCurrentTask = callbackCurrentTask;
        this.runtime = runtime;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> details = callbackCurrentTask.toMap();
        details.put(RUNTIME, runtime.toMap());
        return details;
    }
}
