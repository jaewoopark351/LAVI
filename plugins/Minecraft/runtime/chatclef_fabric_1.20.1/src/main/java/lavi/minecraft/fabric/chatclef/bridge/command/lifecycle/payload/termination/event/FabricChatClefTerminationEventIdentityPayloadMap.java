package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep the termination event identity key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationEventIdentityPayloadMap {
    private static final String EVENT_IDENTITY = "event_identity";

    private FabricChatClefTerminationEventIdentityPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String eventIdentity) {
        payload.put(EVENT_IDENTITY, eventIdentity);
    }
}
