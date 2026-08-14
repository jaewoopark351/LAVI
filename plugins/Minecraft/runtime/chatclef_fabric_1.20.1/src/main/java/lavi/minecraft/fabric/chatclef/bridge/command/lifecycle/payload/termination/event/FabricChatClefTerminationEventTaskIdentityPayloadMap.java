package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep the termination event task-identity key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationEventTaskIdentityPayloadMap {
    private static final String EVENT_TASK_IDENTITY = "event_task_identity";

    private FabricChatClefTerminationEventTaskIdentityPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String eventTaskIdentity) {
        payload.put(EVENT_TASK_IDENTITY, eventTaskIdentity);
    }
}
