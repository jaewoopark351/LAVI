package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.change;

import java.util.Map;

//20260814_kpopmodder: Keep the root-change key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationRootChangedPayloadMap {
    private static final String ROOT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE =
            "root_changed_between_observe_and_dequeue";

    private FabricChatClefTerminationRootChangedPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean rootChangedBetweenObserveAndDequeue) {
        payload.put(ROOT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE, rootChangedBetweenObserveAndDequeue);
    }
}
