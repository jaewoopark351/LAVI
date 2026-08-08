package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.runtime;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

//20260809_kpopmodder: Keep task-finished runtime snapshot fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedRuntimeSnapshotPayloadMap {
    private static final String RUNTIME = "runtime";

    private FabricChatClefTaskFinishedRuntimeSnapshotPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefTaskRuntimeObservationPayload runtime) {
        payload.put(RUNTIME, runtime.toMap());
    }
}
