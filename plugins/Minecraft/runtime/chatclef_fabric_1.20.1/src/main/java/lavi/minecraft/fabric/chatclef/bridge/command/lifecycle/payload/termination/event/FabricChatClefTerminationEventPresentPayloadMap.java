package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep the termination event-present key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationEventPresentPayloadMap {
    private static final String EVENT_PRESENT = "event_present";

    private FabricChatClefTerminationEventPresentPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean eventPresent) {
        payload.put(EVENT_PRESENT, eventPresent);
    }
}
