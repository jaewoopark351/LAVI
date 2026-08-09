package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.snapshot.state;

import java.util.Map;

//20260809_kpopmodder: Keep task-state boolean flags separate without changing snapshot keys.
public final class FabricChatClefTaskSnapshotStateFlagsPayloadMap {
    private static final String TASK_ACTIVE = "task_active";
    private static final String TASK_STOPPED = "task_stopped";
    private static final String THIS_OR_CHILD_TIMED_OUT = "this_or_child_timed_out";

    private FabricChatClefTaskSnapshotStateFlagsPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskActive,
            boolean taskStopped,
            boolean thisOrChildTimedOut
    ) {
        payload.put(TASK_ACTIVE, taskActive);
        payload.put(TASK_STOPPED, taskStopped);
        payload.put(THIS_OR_CHILD_TIMED_OUT, thisOrChildTimedOut);
    }
}
