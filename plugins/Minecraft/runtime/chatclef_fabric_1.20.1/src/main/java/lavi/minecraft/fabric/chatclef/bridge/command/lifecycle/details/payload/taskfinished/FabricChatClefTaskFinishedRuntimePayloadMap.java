package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

//20260808_kpopmodder: Keep task-finished callback/runtime fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedRuntimePayloadMap {
    private static final String FINISH_CALLBACK_RECEIVED = "finish_callback_received";
    private static final String RUNTIME = "runtime";

    private FabricChatClefTaskFinishedRuntimePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        payload.put(FINISH_CALLBACK_RECEIVED, finishCallbackReceived);
        payload.put(RUNTIME, runtime.toMap());
    }
}
