package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate termination observation Map keys without changing emitted diagnostic fields.
public final class FabricChatClefCommandTerminationObservationPayloadMap {
    private static final String COMPLETION_SOURCE = "completion_source";
    private static final String COMPLETION_SOURCE_VALUE = "altoclef_task_finished_event";
    private static final String TERMINATION_KIND = "termination_kind";
    private static final String TASK_PRESENT = "task_present";
    private static final String TASK_STOPPED = "task_stopped";
    private static final String STOP_STATE_AVAILABLE = "stop_state_available";
    private static final String STOP_STATE_ERROR = "stop_state_error";
    private static final String DURATION_SECONDS = "duration_seconds";
    private static final String EVENT_PRESENT = "event_present";
    private static final String EVENT_IDENTITY = "event_identity";
    private static final String EVENT_TASK_PRESENT = "event_task_present";
    private static final String EVENT_TASK_CLASS = "event_task_class";
    private static final String EVENT_TASK_IDENTITY = "event_task_identity";
    private static final String TASK_ABSENCE_REASON = "task_absence_reason";
    private static final String OBSERVATION_SEQUENCE = "observation_sequence";
    private static final String OBSERVED_AT_MS = "observed_at_ms";
    private static final String OBSERVED_CLIENT_TICK = "observed_client_tick";
    private static final String OBSERVATION_THREAD = "observation_thread";
    private static final String QUEUE_DEPTH_BEFORE = "queue_depth_before";
    private static final String QUEUE_DEPTH_AFTER = "queue_depth_after";
    private static final String DEQUEUED_AT_MS = "dequeued_at_ms";
    private static final String DEQUEUED_CLIENT_TICK = "dequeued_client_tick";
    private static final String OBSERVATION_AGE_MS = "observation_age_ms";
    private static final String QUEUE_DEPTH_AFTER_DEQUEUE = "queue_depth_after_dequeue";
    private static final String TASK = "task";
    private static final String OWNERSHIP_AT_OBSERVE = "ownership_at_observe";
    private static final String OWNERSHIP_AT_DEQUEUE = "ownership_at_dequeue";
    private static final String ROOT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE = "root_changed_between_observe_and_dequeue";
    private static final String ROOT_ASSIGNMENT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE = "root_assignment_changed_between_observe_and_dequeue";

    private FabricChatClefCommandTerminationObservationPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String terminationKind,
            boolean taskPresent,
            boolean taskStopped,
            boolean stopStateAvailable,
            String stopStateError,
            double durationSeconds,
            boolean eventPresent,
            String eventIdentity,
            boolean eventTaskPresent,
            String eventTaskClass,
            String eventTaskIdentity,
            String taskAbsenceReason,
            long observationSequence,
            long observedAtMs,
            long observedClientTick,
            String observationThread,
            int queueDepthBefore,
            int queueDepthAfter,
            long dequeuedAtMs,
            long dequeuedClientTick,
            long observationAgeMs,
            int queueDepthAfterDequeue,
            FabricChatClefTaskSnapshot taskSnapshot,
            FabricChatClefTaskOwnershipSnapshot ownershipAtObserve,
            FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue,
            boolean rootChangedBetweenObserveAndDequeue,
            boolean rootAssignmentChangedBetweenObserveAndDequeue
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(COMPLETION_SOURCE, COMPLETION_SOURCE_VALUE);
        payload.put(TERMINATION_KIND, terminationKind);
        payload.put(TASK_PRESENT, taskPresent);
        payload.put(TASK_STOPPED, taskStopped);
        payload.put(STOP_STATE_AVAILABLE, stopStateAvailable);
        payload.put(STOP_STATE_ERROR, stopStateError);
        payload.put(DURATION_SECONDS, durationSeconds);
        payload.put(EVENT_PRESENT, eventPresent);
        payload.put(EVENT_IDENTITY, eventIdentity);
        payload.put(EVENT_TASK_PRESENT, eventTaskPresent);
        payload.put(EVENT_TASK_CLASS, eventTaskClass);
        payload.put(EVENT_TASK_IDENTITY, eventTaskIdentity);
        payload.put(TASK_ABSENCE_REASON, taskAbsenceReason);
        payload.put(OBSERVATION_SEQUENCE, observationSequence);
        payload.put(OBSERVED_AT_MS, observedAtMs);
        payload.put(OBSERVED_CLIENT_TICK, observedClientTick);
        payload.put(OBSERVATION_THREAD, observationThread);
        payload.put(QUEUE_DEPTH_BEFORE, queueDepthBefore);
        payload.put(QUEUE_DEPTH_AFTER, queueDepthAfter);
        payload.put(DEQUEUED_AT_MS, dequeuedAtMs);
        payload.put(DEQUEUED_CLIENT_TICK, dequeuedClientTick);
        payload.put(OBSERVATION_AGE_MS, observationAgeMs);
        payload.put(QUEUE_DEPTH_AFTER_DEQUEUE, queueDepthAfterDequeue);
        payload.put(TASK, taskSnapshot.toMap());
        payload.put(OWNERSHIP_AT_OBSERVE, ownershipAtObserve.toMap());
        payload.put(OWNERSHIP_AT_DEQUEUE, ownershipAtDequeue.toMap());
        payload.put(ROOT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE, rootChangedBetweenObserveAndDequeue);
        payload.put(ROOT_ASSIGNMENT_CHANGED_BETWEEN_OBSERVE_AND_DEQUEUE, rootAssignmentChangedBetweenObserveAndDequeue);
        return payload;
    }
}
