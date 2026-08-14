package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state;

import java.util.Map;

//20260814_kpopmodder: Keep termination stop-state fields separate without changing emitted keys.
public final class FabricChatClefTerminationStopStatePayloadMap {
    private static final String TASK_PRESENT = "task_present";
    private static final String TASK_STOPPED = "task_stopped";
    private static final String STOP_STATE_AVAILABLE = "stop_state_available";
    private static final String STOP_STATE_ERROR = "stop_state_error";
    private static final String DURATION_SECONDS = "duration_seconds";

    private FabricChatClefTerminationStopStatePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskPresent,
            boolean taskStopped,
            boolean stopStateAvailable,
            String stopStateError,
            double durationSeconds
    ) {
        payload.put(TASK_PRESENT, taskPresent);
        payload.put(TASK_STOPPED, taskStopped);
        payload.put(STOP_STATE_AVAILABLE, stopStateAvailable);
        payload.put(STOP_STATE_ERROR, stopStateError);
        payload.put(DURATION_SECONDS, durationSeconds);
    }
}
