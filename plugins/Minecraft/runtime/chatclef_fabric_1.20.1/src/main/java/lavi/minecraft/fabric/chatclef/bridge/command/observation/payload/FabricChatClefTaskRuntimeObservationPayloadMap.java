package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.FabricChatClefTaskRuntimeCapturePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.FabricChatClefTaskRuntimeCurrentTaskPayloadMap;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized runtime task observation Map serialization without changing emitted fields.
public final class FabricChatClefTaskRuntimeObservationPayloadMap {
    private FabricChatClefTaskRuntimeObservationPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String threadName,
            long observedAtMs,
            long clientTickId,
            FabricChatClefTaskSnapshot currentTask,
            FabricChatClefTaskOwnershipSnapshot ownership
    ) {
        Map<String, Object> payload = new HashMap<>();
        FabricChatClefTaskRuntimeCapturePayloadMap.writeTo(
                payload,
                threadName,
                observedAtMs,
                clientTickId
        );
        FabricChatClefTaskRuntimeCurrentTaskPayloadMap.writeTo(
                payload,
                currentTask,
                ownership
        );
        return payload;
    }
}
