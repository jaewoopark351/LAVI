package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.Map;

//20260814_kpopmodder: Keep observe/dequeue ownership snapshots separate without changing emitted keys.
public final class FabricChatClefTerminationOwnershipSnapshotPayloadMap {
    private static final String OWNERSHIP_AT_OBSERVE = "ownership_at_observe";
    private static final String OWNERSHIP_AT_DEQUEUE = "ownership_at_dequeue";

    private FabricChatClefTerminationOwnershipSnapshotPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskOwnershipSnapshot ownershipAtObserve,
            FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue
    ) {
        payload.put(OWNERSHIP_AT_OBSERVE, ownershipAtObserve.toMap());
        payload.put(OWNERSHIP_AT_DEQUEUE, ownershipAtDequeue.toMap());
    }
}
