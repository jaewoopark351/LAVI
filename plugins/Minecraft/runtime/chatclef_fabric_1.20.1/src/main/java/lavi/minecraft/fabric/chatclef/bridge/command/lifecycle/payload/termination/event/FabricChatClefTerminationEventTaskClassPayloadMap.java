package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep the termination event task-class key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationEventTaskClassPayloadMap {
    private static final String EVENT_TASK_CLASS = "event_task_class";

    private FabricChatClefTerminationEventTaskClassPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String eventTaskClass) {
        payload.put(EVENT_TASK_CLASS, eventTaskClass);
    }
}
