package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

//20260808_kpopmodder: Keep terminal-wait runtime fields separate without changing emitted keys.
public final class FabricChatClefWaitingRuntimePayloadMap {
    private static final String RUNTIME = "runtime";

    private FabricChatClefWaitingRuntimePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> details,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        details.put(RUNTIME, runtime.toMap());
    }
}
