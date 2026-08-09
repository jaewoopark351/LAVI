package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state;

import java.util.Map;

//20260809_kpopmodder: Keep task-state availability serialization separate without changing snapshot keys.
public final class FabricChatClefTaskSnapshotStateAvailabilityPayloadMap {
    private static final String TASK_STATE_AVAILABLE = "task_state_available";

    private FabricChatClefTaskSnapshotStateAvailabilityPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskStateAvailable
    ) {
        payload.put(TASK_STATE_AVAILABLE, taskStateAvailable);
    }
}
