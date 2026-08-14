package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue;

import java.util.Map;

//20260814_kpopmodder: Keep the dequeue client-tick key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationDequeuedClientTickPayloadMap {
    private static final String DEQUEUED_CLIENT_TICK = "dequeued_client_tick";

    private FabricChatClefTerminationDequeuedClientTickPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long dequeuedClientTick) {
        payload.put(DEQUEUED_CLIENT_TICK, dequeuedClientTick);
    }
}
