package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.FabricChatClefCommandTerminationObservationPayload;
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
    private final long observedAtMs;
    private final String observationThread;
    private final FabricChatClefTaskSnapshot taskSnapshot;

    private FabricChatClefCommandTerminationObservation(
            Task task,
            boolean taskPresent,
            boolean taskStopped,
            boolean stopStateAvailable,
            String stopStateError,
            double durationSeconds,
            long observedAtMs,
            String observationThread,
            FabricChatClefTaskSnapshot taskSnapshot
    ) {
        this.task = task;
        this.taskPresent = taskPresent;
        this.taskStopped = taskStopped;
        this.stopStateAvailable = stopStateAvailable;
        this.stopStateError = stopStateError;
        this.durationSeconds = durationSeconds;
        this.observedAtMs = observedAtMs;
        this.observationThread = observationThread;
        this.taskSnapshot = taskSnapshot;
    }

    public static FabricChatClefCommandTerminationObservation fromTaskFinishedEvent(TaskFinishedEvent event) {
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
                System.currentTimeMillis(),
                Thread.currentThread().getName(),
                taskSnapshot
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

    public String terminationKind() {
        if (!taskPresent) {
            return "cancelled_without_task";
        }
        if (!stopStateAvailable) {
            return "unknown_stop_state";
        }
        return taskStopped ? "stopped" : "finished";
    }

    public Map<String, Object> toMap() {
        return new FabricChatClefCommandTerminationObservationPayload(
                terminationKind(),
                taskPresent,
                taskStopped,
                stopStateAvailable,
                stopStateError,
                durationSeconds,
                observedAtMs,
                observationThread,
                taskSnapshot
        ).toMap();
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
