package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.change;

import java.util.Map;

//20260814_kpopmodder: Keep the root-assignment-change key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationRootAssignmentChangedPayloadMap {
    private static final String ROOT_ASSIGNMENT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE =
            "root_assignment_changed_between_observe_and_dequeue";

    private FabricChatClefTerminationRootAssignmentChangedPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean rootAssignmentChangedBetweenObserveAndDequeue) {
        payload.put(ROOT_ASSIGNMENT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE, rootAssignmentChangedBetweenObserveAndDequeue);
    }
}
