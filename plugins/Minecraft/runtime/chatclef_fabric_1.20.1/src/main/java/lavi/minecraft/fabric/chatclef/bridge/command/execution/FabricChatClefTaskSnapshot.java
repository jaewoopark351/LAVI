package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Observe ChatClef task identity without owning or mutating engine state.
public final class FabricChatClefTaskSnapshot {
    private final boolean available;
    private final String className;
    private final String description;
    private final String identity;
    private final String error;
    private final boolean taskStateAvailable;
    private final boolean taskActive;
    private final boolean taskStopped;
    private final boolean thisOrChildTimedOut;
    private final String taskStateError;

    private FabricChatClefTaskSnapshot(
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
        this.available = available;
        this.className = className;
        this.description = description;
        this.identity = identity;
        this.error = error;
        this.taskStateAvailable = taskStateAvailable;
        this.taskActive = taskActive;
        this.taskStopped = taskStopped;
        this.thisOrChildTimedOut = thisOrChildTimedOut;
        this.taskStateError = taskStateError;
    }

    public static FabricChatClefTaskSnapshot capture(Object task) {
        if (task == null) {
            return new FabricChatClefTaskSnapshot(false, "", "", "", "", false, false, false, false, "");
        }
        TaskState taskState = captureTaskState(task);
        return new FabricChatClefTaskSnapshot(
                true,
                task.getClass().getName(),
                safeDescription(task),
                Integer.toHexString(System.identityHashCode(task)),
                "",
                taskState.available,
                taskState.active,
                taskState.stopped,
                taskState.thisOrChildTimedOut,
                taskState.error
        );
    }

    public static FabricChatClefTaskSnapshot unavailable(Throwable error) {
        return new FabricChatClefTaskSnapshot(
                false,
                "",
                "",
                "",
                error.getClass().getSimpleName() + ": " + nullSafeMessage(error),
                false,
                false,
                false,
                false,
                ""
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("available", available);
        payload.put("class_name", className);
        payload.put("description", description);
        payload.put("identity", identity);
        payload.put("error", error);
        payload.put("task_state_available", taskStateAvailable);
        payload.put("task_active", taskActive);
        payload.put("task_stopped", taskStopped);
        payload.put("this_or_child_timed_out", thisOrChildTimedOut);
        payload.put("task_state_error", taskStateError);
        return payload;
    }

    private static TaskState captureTaskState(Object task) {
        if (!(task instanceof Task typedTask)) {
            return TaskState.unavailable("");
        }
        try {
            return new TaskState(
                    true,
                    typedTask.isActive(),
                    typedTask.stopped(),
                    typedTask.thisOrChildAreTimedOut(),
                    ""
            );
        } catch (Throwable error) {
            return TaskState.unavailable(error.getClass().getSimpleName() + ": " + nullSafeMessage(error));
        }
    }

    private static String safeDescription(Object task) {
        try {
            return String.valueOf(task);
        } catch (Throwable error) {
            return "<toString failed: " + error.getClass().getSimpleName() + ">";
        }
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }

    private static final class TaskState {
        private final boolean available;
        private final boolean active;
        private final boolean stopped;
        private final boolean thisOrChildTimedOut;
        private final String error;

        private TaskState(
                boolean available,
                boolean active,
                boolean stopped,
                boolean thisOrChildTimedOut,
                String error
        ) {
            this.available = available;
            this.active = active;
            this.stopped = stopped;
            this.thisOrChildTimedOut = thisOrChildTimedOut;
            this.error = error;
        }

        private static TaskState unavailable(String error) {
            return new TaskState(false, false, false, false, error);
        }
    }
}
