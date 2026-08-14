package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation;

import java.util.Map;

//20260814_kpopmodder: Keep the observed client-tick key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationObservedClientTickPayloadMap {
    private static final String OBSERVED_CLIENT_TICK = "observed_client_tick";

    private FabricChatClefTerminationObservedClientTickPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long observedClientTick) {
        payload.put(OBSERVED_CLIENT_TICK, observedClientTick);
    }
}
