package lavi.minecraft.diagnostics;

import adris.altoclef.tasksystem.Task;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicReference;

//20260731_kpopmodder: Own diagnostic task identity and task stack tracking separately from log emission.
final class DiagnosticTaskRegistry {
    private final IdentityHashMap<Object, Long> taskInstanceIds = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Long> taskRunIds = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Long> parentTaskRunIds = new IdentityHashMap<>();
    private final AtomicReference<Object> modeEpoch = new AtomicReference<>(new Object());
    private final ThreadLocal<EpochTaskStack> taskStack = new ThreadLocal<>();
    private long nextTaskInstanceId = 1;
    private long nextTaskRunId = 1;

    void enterTask(Task task) {
        try {
            currentTaskStack().push(task);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    void exitTask(Task task) {
        try {
            Deque<Task> stack = currentTaskStack();
            if (!stack.isEmpty() && stack.peek() == task) {
                stack.pop();
                return;
            }
            stack.remove(task);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    Task currentTask() {
        try {
            Deque<Task> stack = currentTaskStack();
            return stack.isEmpty() ? null : stack.peek();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    synchronized void setParent(Task child, Task parent) {
        long parentRunId = existingRunId(parent);
        if (child != null) {
            parentTaskRunIds.put(child, parentRunId);
        }
    }

    synchronized long instanceId(Object task) {
        Long existing = taskInstanceIds.get(task);
        if (existing != null) {
            return existing;
        }
        long created = nextTaskInstanceId++;
        taskInstanceIds.put(task, created);
        return created;
    }

    synchronized long existingRunId(Object task) {
        Long existing = taskRunIds.get(task);
        return existing == null ? -1 : existing;
    }

    synchronized long createRunId(Object task) {
        long created = nextTaskRunId++;
        taskRunIds.put(task, created);
        return created;
    }

    synchronized long parentRunId(Object task) {
        Long existing = parentTaskRunIds.get(task);
        return existing == null ? -1 : existing;
    }

    synchronized String taskInstanceIdLabel(Task task) {
        return task == null ? "unavailable" : idLabel(instanceId(task));
    }

    synchronized String taskRunIdLabel(Task task) {
        return task == null ? "unavailable" : idLabel(existingRunId(task));
    }

    synchronized void clearForModeTransition() {
        taskInstanceIds.clear();
        taskRunIds.clear();
        parentTaskRunIds.clear();
        modeEpoch.set(new Object());
        taskStack.remove();
    }

    private Deque<Task> currentTaskStack() {
        Object currentEpoch = modeEpoch.get();
        EpochTaskStack current = taskStack.get();
        if (current == null || current.modeEpoch != currentEpoch) {
            current = new EpochTaskStack(currentEpoch, new ArrayDeque<>());
            taskStack.set(current);
        }
        return current.tasks;
    }

    private record EpochTaskStack(Object modeEpoch, Deque<Task> tasks) {
    }

    static String idLabel(long id) {
        return id < 0 ? "unavailable" : Long.toString(id);
    }
}
