package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state;

import java.util.Map;

//20260809_kpopmodder: Keep task-state error serialization separate without changing snapshot keys.
public final class FabricChatClefTaskSnapshotStateErrorPayloadMap {
    private static final String TASK_STATE_ERROR = "task_state_error";

    private FabricChatClefTaskSnapshotStateErrorPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String taskStateError
    ) {
        payload.put(TASK_STATE_ERROR, taskStateError);
    }
}
