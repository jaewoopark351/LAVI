package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.runtime.FabricChatClefTaskFinishedCallbackPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.runtime.FabricChatClefTaskFinishedRuntimeSnapshotPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

//20260808_kpopmodder: Keep task-finished callback/runtime fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedRuntimePayloadMap {
    private FabricChatClefTaskFinishedRuntimePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        FabricChatClefTaskFinishedCallbackPayloadMap.writeTo(payload, finishCallbackReceived);
        FabricChatClefTaskFinishedRuntimeSnapshotPayloadMap.writeTo(payload, runtime);
    }
}
