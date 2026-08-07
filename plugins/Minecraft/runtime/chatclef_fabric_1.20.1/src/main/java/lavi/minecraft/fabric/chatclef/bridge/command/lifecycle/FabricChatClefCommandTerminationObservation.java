package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.FabricChatClefCommandTerminationObservationPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260803_kpopmodder: Observe user task termination without changing ChatClef engine behavior.
public final class FabricChatClefCommandTerminationObservation {
    private final Task task;
    private final boolean taskPresent;
    private final boolean taskStopped;
    private final boolean stopStateAvailable;
    private final String stopStateError;
    private final double durationSeconds;
    private final boolean eventPresent;
    private final String eventIdentity;
    private final boolean eventTaskPresent;
    private final String eventTaskClass;
    private final String eventTaskIdentity;
    private final String taskAbsenceReason;
    private final long observationSequence;
    private final long observedAtMs;
    private final long observedClientTick;
    private final String observationThread;
    private final int queueDepthBefore;
    private final int queueDepthAfter;
    private final long dequeuedAtMs;
    private final long dequeuedClientTick;
    private final long observationAgeMs;
    private final int queueDepthAfterDequeue;
    private final FabricChatClefTaskSnapshot taskSnapshot;
    private final FabricChatClefTaskOwnershipSnapshot ownershipAtObserve;
    private final FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue;

    private FabricChatClefCommandTerminationObservation(
            Task task,
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
            FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue
    ) {
        this.task = task;
        this.taskPresent = taskPresent;
        this.taskStopped = taskStopped;
        this.stopStateAvailable = stopStateAvailable;
        this.stopStateError = stopStateError;
        this.durationSeconds = durationSeconds;
        this.eventPresent = eventPresent;
        this.eventIdentity = eventIdentity;
        this.eventTaskPresent = eventTaskPresent;
        this.eventTaskClass = eventTaskClass;
        this.eventTaskIdentity = eventTaskIdentity;
        this.taskAbsenceReason = taskAbsenceReason;
        this.observationSequence = observationSequence;
        this.observedAtMs = observedAtMs;
        this.observedClientTick = observedClientTick;
        this.observationThread = observationThread;
        this.queueDepthBefore = queueDepthBefore;
        this.queueDepthAfter = queueDepthAfter;
        this.dequeuedAtMs = dequeuedAtMs;
        this.dequeuedClientTick = dequeuedClientTick;
        this.observationAgeMs = observationAgeMs;
        this.queueDepthAfterDequeue = queueDepthAfterDequeue;
        this.taskSnapshot = taskSnapshot;
        this.ownershipAtObserve = ownershipAtObserve;
        this.ownershipAtDequeue = ownershipAtDequeue;
    }

    public static FabricChatClefCommandTerminationObservation fromTaskFinishedEvent(TaskFinishedEvent event) {
        return fromTaskFinishedEvent(
                event,
                0L,
                0L,
                -1,
                -1,
                FabricChatClefTaskOwnershipSnapshot.empty()
        );
    }

    public static FabricChatClefCommandTerminationObservation fromTaskFinishedEvent(
            TaskFinishedEvent event,
            long observationSequence,
            long observedClientTick,
            int queueDepthBefore,
            int queueDepthAfter,
            FabricChatClefTaskOwnershipSnapshot ownershipAtObserve
    ) {
        Task task = event == null ? null : event.lastTaskRan;
        boolean taskPresent = task != null;
        boolean taskStopped = false;
        boolean stopStateAvailable = !taskPresent;
        String stopStateError = "";
        FabricChatClefTaskSnapshot taskSnapshot;
        try {
            taskStopped = taskPresent && task.stopped();
            stopStateAvailable = true;
            taskSnapshot = FabricChatClefTaskSnapshot.capture(task);
        } catch (Throwable error) {
            stopStateError = error.getClass().getSimpleName() + ": " + nullSafeMessage(error);
            taskSnapshot = FabricChatClefTaskSnapshot.unavailable(error);
        }
        return new FabricChatClefCommandTerminationObservation(
                task,
                taskPresent,
                taskStopped,
                stopStateAvailable,
                stopStateError,
                event == null ? 0.0 : event.durationSeconds,
                event != null,
                event == null ? "none" : Integer.toHexString(System.identityHashCode(event)),
                taskPresent,
                task == null ? "" : task.getClass().getName(),
                taskIdentity(task),
                taskAbsenceReason(event, task),
                observationSequence,
                System.currentTimeMillis(),
                observedClientTick,
                Thread.currentThread().getName(),
                queueDepthBefore,
                queueDepthAfter,
                0L,
                0L,
                -1L,
                -1,
                taskSnapshot,
                ownershipAtObserve,
                FabricChatClefTaskOwnershipSnapshot.empty()
        );
    }

    public FabricChatClefCommandTerminationObservation withDequeueMetadata(
            long dequeuedAtMs,
            long dequeuedClientTick,
            int queueDepthAfterDequeue,
            FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue
    ) {
        return new FabricChatClefCommandTerminationObservation(
                task,
                taskPresent,
                taskStopped,
                stopStateAvailable,
                stopStateError,
                durationSeconds,
                eventPresent,
                eventIdentity,
                eventTaskPresent,
                eventTaskClass,
                eventTaskIdentity,
                taskAbsenceReason,
                observationSequence,
                observedAtMs,
                observedClientTick,
                observationThread,
                queueDepthBefore,
                queueDepthAfter,
                dequeuedAtMs,
                dequeuedClientTick,
                dequeuedAtMs <= 0L ? -1L : dequeuedAtMs - observedAtMs,
                queueDepthAfterDequeue,
                taskSnapshot,
                ownershipAtObserve,
                ownershipAtDequeue
        );
    }

    public Task task() {
        return task;
    }

    public boolean taskPresent() {
        return taskPresent;
    }

    public boolean taskStopped() {
        return taskStopped;
    }

    public boolean stopStateAvailable() {
        return stopStateAvailable;
    }

    public long observationSequence() {
        return observationSequence;
    }

    public String terminationKind() {
        if (!taskPresent) {
            return "cancelled_without_task";
        }
        if (!stopStateAvailable) {
            return "unknown_stop_state";
        }
        return taskStopped ? "stopped" : "finished";
    }

    public FabricChatClefCommandTerminationObservationPayload payload() {
        return new FabricChatClefCommandTerminationObservationPayload(
                terminationKind(),
                taskPresent,
                taskStopped,
                stopStateAvailable,
                stopStateError,
                durationSeconds,
                eventPresent,
                eventIdentity,
                eventTaskPresent,
                eventTaskClass,
                eventTaskIdentity,
                taskAbsenceReason,
                observationSequence,
                observedAtMs,
                observedClientTick,
                observationThread,
                queueDepthBefore,
                queueDepthAfter,
                dequeuedAtMs,
                dequeuedClientTick,
                observationAgeMs,
                queueDepthAfterDequeue,
                taskSnapshot,
                ownershipAtObserve,
                ownershipAtDequeue,
                rootChangedBetweenObserveAndDequeue(),
                rootAssignmentChangedBetweenObserveAndDequeue()
        );
    }

    public Map<String, Object> toMap() {
        return payload().toMap();
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }

    private boolean rootChangedBetweenObserveAndDequeue() {
        return dequeuedAtMs > 0L
                && ownershipAtDequeue != null
                && !ownershipAtDequeue.userTaskRootIdentity().equals(ownershipAtObserve.userTaskRootIdentity());
    }

    private boolean rootAssignmentChangedBetweenObserveAndDequeue() {
        return dequeuedAtMs > 0L
                && ownershipAtDequeue != null
                && !ownershipAtDequeue.userTaskRootAssignmentId().equals(ownershipAtObserve.userTaskRootAssignmentId());
    }

    private static String taskAbsenceReason(TaskFinishedEvent event, Task task) {
        if (event == null) {
            return "event_null";
        }
        return task == null ? "event_last_task_null" : "task_present";
    }

    private static String taskIdentity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }
}
