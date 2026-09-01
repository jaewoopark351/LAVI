package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasksystem.Task;

import java.lang.reflect.Field;

//20260831_kpopmodder: Isolate reflection needed to prepare and inspect headless Task lifecycle state.
final class TaskLifecycleAccess {
    private TaskLifecycleAccess() {
    }

    static void initialize(Task task) {
        setObject(task, "oldDebugState", "");
        setObject(task, "debugState", "");
        setBoolean(task, "first", true);
        setBoolean(task, "stopped", false);
        setBoolean(task, "active", false);
    }

    static Task actualChild(Task parent) {
        try {
            Field field = Task.class.getDeclaredField("sub");
            field.setAccessible(true);
            return (Task) field.get(parent);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect scheduler-owned child", exception);
        }
    }

    static void markActive(Task task) {
        setBoolean(task, "stopped", false);
        setBoolean(task, "active", true);
    }

    private static void setObject(Task task, String fieldName, Object value) {
        try {
            Field field = Task.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(task, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to initialize Task." + fieldName, exception);
        }
    }

    private static void setBoolean(Task task, String fieldName, boolean value) {
        try {
            Field field = Task.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setBoolean(task, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to initialize Task." + fieldName, exception);
        }
    }
}
