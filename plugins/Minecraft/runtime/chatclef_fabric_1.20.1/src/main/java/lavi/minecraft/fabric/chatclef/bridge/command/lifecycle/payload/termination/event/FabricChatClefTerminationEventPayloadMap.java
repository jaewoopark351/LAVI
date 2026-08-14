package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep termination event identity fields separate without changing emitted keys.
public final class FabricChatClefTerminationEventPayloadMap {
    private FabricChatClefTerminationEventPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean eventPresent,
            String eventIdentity,
            boolean eventTaskPresent,
            String eventTaskClass,
            String eventTaskIdentity,
            String taskAbsenceReason
    ) {
        FabricChatClefTerminationEventPresentPayloadMap.writeTo(payload, eventPresent);
        FabricChatClefTerminationEventIdentityPayloadMap.writeTo(payload, eventIdentity);
        FabricChatClefTerminationEventTaskPresentPayloadMap.writeTo(payload, eventTaskPresent);
        FabricChatClefTerminationEventTaskClassPayloadMap.writeTo(payload, eventTaskClass);
        FabricChatClefTerminationEventTaskIdentityPayloadMap.writeTo(payload, eventTaskIdentity);
        FabricChatClefTerminationTaskAbsenceReasonPayloadMap.writeTo(payload, taskAbsenceReason);
    }
}
