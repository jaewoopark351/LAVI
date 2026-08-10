package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.capture.FabricChatClefTaskRuntimeClientTickPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.capture.FabricChatClefTaskRuntimeObservedAtPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.capture.FabricChatClefTaskRuntimeThreadNamePayloadMap;

import java.util.Map;

//20260808_kpopmodder: Split runtime observation capture fields without changing emitted keys.
public final class FabricChatClefTaskRuntimeCapturePayloadMap {
    private FabricChatClefTaskRuntimeCapturePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String threadName,
            long observedAtMs,
            long clientTickId
    ) {
        FabricChatClefTaskRuntimeThreadNamePayloadMap.writeTo(payload, threadName);
        FabricChatClefTaskRuntimeObservedAtPayloadMap.writeTo(payload, observedAtMs);
        FabricChatClefTaskRuntimeClientTickPayloadMap.writeTo(payload, clientTickId);
    }
}
