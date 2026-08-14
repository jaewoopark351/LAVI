package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership;

import java.util.Map;

//20260814_kpopmodder: Keep root-change flags separate without changing emitted keys.
public final class FabricChatClefTerminationOwnershipChangePayloadMap {
    private static final String ROOT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE =
            "root_changed_between_observe_and_dequeue";
    private static final String ROOT_ASSIGNMENT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE =
            "root_assignment_changed_between_observe_and_dequeue";

    private FabricChatClefTerminationOwnershipChangePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean rootChangedBetweenObserveAndDequeue,
            boolean rootAssignmentChangedBetweenObserveAndDequeue
    ) {
        payload.put(ROOT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE, rootChangedBetweenObserveAndDequeue);
        payload.put(ROOT_ASSIGNMENT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE, rootAssignmentChangedBetweenObserveAndDequeue);
    }
}
