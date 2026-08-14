package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.snapshot.FabricChatClefTerminationOwnershipAtDequeuePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.snapshot.FabricChatClefTerminationOwnershipAtObservePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.Map;

//20260814_kpopmodder: Keep observe/dequeue ownership snapshots separate without changing emitted keys.
public final class FabricChatClefTerminationOwnershipSnapshotPayloadMap {
    private FabricChatClefTerminationOwnershipSnapshotPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskOwnershipSnapshot ownershipAtObserve,
            FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue
    ) {
        FabricChatClefTerminationOwnershipAtObservePayloadMap.writeTo(payload, ownershipAtObserve);
        FabricChatClefTerminationOwnershipAtDequeuePayloadMap.writeTo(payload, ownershipAtDequeue);
    }
}
