package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.finish.runtime.FabricChatClefFinishCallbackRuntimeSnapshotPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

//20260808_kpopmodder: Keep finish-callback runtime fields separate without changing emitted keys.
public final class FabricChatClefFinishCallbackRuntimePayloadMap {
    private FabricChatClefFinishCallbackRuntimePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> details,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        FabricChatClefFinishCallbackRuntimeSnapshotPayloadMap.writeTo(details, runtime);
    }
}
