package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish.runtime;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

//20260809_kpopmodder: Keep finish-callback runtime snapshot fields separate without changing emitted keys.
public final class FabricChatClefFinishCallbackRuntimeSnapshotPayloadMap {
    private static final String RUNTIME = "runtime";

    private FabricChatClefFinishCallbackRuntimeSnapshotPayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, FabricChatClefTaskRuntimeObservationPayload runtime) {
        details.put(RUNTIME, runtime.toMap());
    }
}
