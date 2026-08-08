package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot;

import java.util.Map;

//20260808_kpopmodder: Keep task runtime state fields separate at the task snapshot Map edge.
public final class FabricChatClefTaskSnapshotStatePayloadMap {
    private static final String TASK_STATE_AVAILABLE = "task_state_available";
    private static final String TASK_ACTIVE = "task_active";
    private static final String TASK_STOPPED = "task_stopped";
    private static final String THIS_OR_CHILD_TIMED_OUT = "this_or_child_timed_out";
    private static final String TASK_STATE_ERROR = "task_state_error";

    private FabricChatClefTaskSnapshotStatePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskStateAvailable,
            boolean taskActive,
            boolean taskStopped,
            boolean thisOrChildTimedOut,
            String taskStateError
    ) {
        payload.put(TASK_STATE_AVAILABLE, taskStateAvailable);
        payload.put(TASK_ACTIVE, taskActive);
        payload.put(TASK_STOPPED, taskStopped);
        payload.put(THIS_OR_CHILD_TIMED_OUT, thisOrChildTimedOut);
        payload.put(TASK_STATE_ERROR, taskStateError);
    }
}
