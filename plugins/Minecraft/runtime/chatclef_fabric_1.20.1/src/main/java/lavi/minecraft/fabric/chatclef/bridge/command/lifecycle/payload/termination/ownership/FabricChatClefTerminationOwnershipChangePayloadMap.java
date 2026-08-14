package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.change.FabricChatClefTerminationRootAssignmentChangedPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.change.FabricChatClefTerminationRootChangedPayloadMap;

import java.util.Map;

//20260814_kpopmodder: Keep root-change flags separate without changing emitted keys.
public final class FabricChatClefTerminationOwnershipChangePayloadMap {
    private FabricChatClefTerminationOwnershipChangePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean rootChangedBetweenObserveAndDequeue,
            boolean rootAssignmentChangedBetweenObserveAndDequeue
    ) {
        FabricChatClefTerminationRootChangedPayloadMap.writeTo(payload, rootChangedBetweenObserveAndDequeue);
        FabricChatClefTerminationRootAssignmentChangedPayloadMap.writeTo(
                payload,
                rootAssignmentChangedBetweenObserveAndDequeue
        );
    }
}
