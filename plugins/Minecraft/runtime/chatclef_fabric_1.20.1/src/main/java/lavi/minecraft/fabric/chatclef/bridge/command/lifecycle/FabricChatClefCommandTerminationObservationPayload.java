package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefTaskSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Isolate termination observation diagnostic payload keys without changing emitted fields.
final class FabricChatClefCommandTerminationObservationPayload {
    private static final String COMPLETION_SOURCE = "completion_source";
    private static final String COMPLETION_SOURCE_VALUE = "altoclef_task_finished_event";
    private static final String TERMINATION_KIND = "termination_kind";
    private static final String TASK_PRESENT = "task_present";
    private static final String TASK_STOPPED = "task_stopped";
    private static final String STOP_STATE_AVAILABLE = "stop_state_available";
    private static final String STOP_STATE_ERROR = "stop_state_error";
    private static final String DURATION_SECONDS = "duration_seconds";
    private static final String OBSERVED_AT_MS = "observed_at_ms";
    private static final String OBSERVATION_THREAD = "observation_thread";
    private static final String TASK = "task";

    private final String terminationKind;
    private final boolean taskPresent;
    private final boolean taskStopped;
    private final boolean stopStateAvailable;
    private final String stopStateError;
    private final double durationSeconds;
    private final long observedAtMs;
    private final String observationThread;
    private final FabricChatClefTaskSnapshot taskSnapshot;

    FabricChatClefCommandTerminationObservationPayload(
            String terminationKind,
            boolean taskPresent,
            boolean taskStopped,
            boolean stopStateAvailable,
            String stopStateError,
            double durationSeconds,
            long observedAtMs,
            String observationThread,
            FabricChatClefTaskSnapshot taskSnapshot
    ) {
        this.terminationKind = terminationKind;
        this.taskPresent = taskPresent;
        this.taskStopped = taskStopped;
        this.stopStateAvailable = stopStateAvailable;
        this.stopStateError = stopStateError;
        this.durationSeconds = durationSeconds;
        this.observedAtMs = observedAtMs;
        this.observationThread = observationThread;
        this.taskSnapshot = taskSnapshot;
    }

    Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put(COMPLETION_SOURCE, COMPLETION_SOURCE_VALUE);
        payload.put(TERMINATION_KIND, terminationKind);
        payload.put(TASK_PRESENT, taskPresent);
        payload.put(TASK_STOPPED, taskStopped);
        payload.put(STOP_STATE_AVAILABLE, stopStateAvailable);
        payload.put(STOP_STATE_ERROR, stopStateError);
        payload.put(DURATION_SECONDS, durationSeconds);
        payload.put(OBSERVED_AT_MS, observedAtMs);
        payload.put(OBSERVATION_THREAD, observationThread);
        payload.put(TASK, taskSnapshot.toMap());
        return payload;
    }
}
