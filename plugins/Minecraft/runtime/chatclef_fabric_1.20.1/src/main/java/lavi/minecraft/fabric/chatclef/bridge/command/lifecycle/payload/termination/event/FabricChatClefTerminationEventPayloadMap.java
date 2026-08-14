package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep termination event identity fields separate without changing emitted keys.
public final class FabricChatClefTerminationEventPayloadMap {
    private static final String EVENT_PRESENT = "event_present";
    private static final String EVENT_IDENTITY = "event_identity";
    private static final String EVENT_TASK_PRESENT = "event_task_present";
    private static final String EVENT_TASK_CLASS = "event_task_class";
    private static final String EVENT_TASK_IDENTITY = "event_task_identity";
    private static final String TASK_ABSENCE_REASON = "task_absence_reason";

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
        payload.put(EVENT_PRESENT, eventPresent);
        payload.put(EVENT_IDENTITY, eventIdentity);
        payload.put(EVENT_TASK_PRESENT, eventTaskPresent);
        payload.put(EVENT_TASK_CLASS, eventTaskClass);
        payload.put(EVENT_TASK_IDENTITY, eventTaskIdentity);
        payload.put(TASK_ABSENCE_REASON, taskAbsenceReason);
    }
}
