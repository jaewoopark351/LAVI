package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate task snapshot Map keys without changing emitted diagnostic fields.
public final class FabricChatClefTaskSnapshotPayloadMap {
    private static final String AVAILABLE = "available";
    private static final String CLASS_NAME = "class_name";
    private static final String DESCRIPTION = "description";
    private static final String IDENTITY = "identity";
    private static final String ERROR = "error";
    private static final String TASK_STATE_AVAILABLE = "task_state_available";
    private static final String TASK_ACTIVE = "task_active";
    private static final String TASK_STOPPED = "task_stopped";
    private static final String THIS_OR_CHILD_TIMED_OUT = "this_or_child_timed_out";
    private static final String TASK_STATE_ERROR = "task_state_error";

    private FabricChatClefTaskSnapshotPayloadMap() {
    }

    public static Map<String, Object> toMap(
            boolean available,
            String className,
            String description,
            String identity,
            String error,
            boolean taskStateAvailable,
            boolean taskActive,
            boolean taskStopped,
            boolean thisOrChildTimedOut,
            String taskStateError
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(AVAILABLE, available);
        payload.put(CLASS_NAME, className);
        payload.put(DESCRIPTION, description);
        payload.put(IDENTITY, identity);
        payload.put(ERROR, error);
        payload.put(TASK_STATE_AVAILABLE, taskStateAvailable);
        payload.put(TASK_ACTIVE, taskActive);
        payload.put(TASK_STOPPED, taskStopped);
        payload.put(THIS_OR_CHILD_TIMED_OUT, thisOrChildTimedOut);
        payload.put(TASK_STATE_ERROR, taskStateError);
        return payload;
    }
}
