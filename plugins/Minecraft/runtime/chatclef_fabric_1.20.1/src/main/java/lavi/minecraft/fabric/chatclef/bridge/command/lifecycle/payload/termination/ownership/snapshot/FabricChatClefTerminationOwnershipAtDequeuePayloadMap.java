package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.snapshot;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.Map;

//20260814_kpopmodder: Keep the dequeue ownership snapshot key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationOwnershipAtDequeuePayloadMap {
    private static final String OWNERSHIP_AT_DEQUEUE = "ownership_at_dequeue";

    private FabricChatClefTerminationOwnershipAtDequeuePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue) {
        payload.put(OWNERSHIP_AT_DEQUEUE, ownershipAtDequeue.toMap());
    }
}
