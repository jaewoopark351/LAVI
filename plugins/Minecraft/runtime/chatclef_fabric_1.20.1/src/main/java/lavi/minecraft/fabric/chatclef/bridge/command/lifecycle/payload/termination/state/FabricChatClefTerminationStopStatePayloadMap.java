package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state;

import java.util.Map;

//20260814_kpopmodder: Keep termination stop-state fields separate without changing emitted keys.
public final class FabricChatClefTerminationStopStatePayloadMap {
    private FabricChatClefTerminationStopStatePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskPresent,
            boolean taskStopped,
            boolean stopStateAvailable,
            String stopStateError,
            double durationSeconds
    ) {
        FabricChatClefTerminationTaskPresentPayloadMap.writeTo(payload, taskPresent);
        FabricChatClefTerminationTaskStoppedPayloadMap.writeTo(payload, taskStopped);
        FabricChatClefTerminationStopStateAvailablePayloadMap.writeTo(payload, stopStateAvailable);
        FabricChatClefTerminationStopStateErrorPayloadMap.writeTo(payload, stopStateError);
        FabricChatClefTerminationDurationSecondsPayloadMap.writeTo(payload, durationSeconds);
    }
}
