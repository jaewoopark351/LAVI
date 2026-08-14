package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.snapshot;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.Map;

//20260814_kpopmodder: Keep the observe ownership snapshot key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationOwnershipAtObservePayloadMap {
    private static final String OWNERSHIP_AT_OBSERVE = "ownership_at_observe";

    private FabricChatClefTerminationOwnershipAtObservePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefTaskOwnershipSnapshot ownershipAtObserve) {
        payload.put(OWNERSHIP_AT_OBSERVE, ownershipAtObserve.toMap());
    }
}
