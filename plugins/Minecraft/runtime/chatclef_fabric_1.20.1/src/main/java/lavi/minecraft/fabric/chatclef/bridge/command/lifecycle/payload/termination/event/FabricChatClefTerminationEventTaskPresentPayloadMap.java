package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep the termination event task-present key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationEventTaskPresentPayloadMap {
    private static final String EVENT_TASK_PRESENT = "event_task_present";

    private FabricChatClefTerminationEventTaskPresentPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean eventTaskPresent) {
        payload.put(EVENT_TASK_PRESENT, eventTaskPresent);
    }
}
