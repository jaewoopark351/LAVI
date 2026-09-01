package lavi.minecraft.diagnostics.tasktrace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementSourceEventObserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

//20260801_kpopmodder: Added bounded task-stack diagnostics for the visible furnace/fuel/mining task chain.
public final class VisibleTaskDiagnostics {
    private static final int MAX_STATE_KEY_LENGTH = 220;
    private static final Map<Task, Map<String, String>> LAST_STATE_BY_TASK =
            Collections.synchronizedMap(new WeakHashMap<>());

    private VisibleTaskDiagnostics() {
    }

    public static void logLifecycle(AltoClef mod, Task task, String phase, String reason, Object... fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        ChatClefDiagnostics.logBoundary("VISIBLE_TASK_LIFECYCLE", reason, task,
                mergeCommonFields(mod, mergeFields(new Object[]{"phase", phase}, fields)));
    }

    public static void logDecision(AltoClef mod, Task task, String reason, String stateKey, Object... fields) {
        logStateChange("VISIBLE_TASK_DECISION", mod, task, reason, stateKey, fields);
    }

    public static void logProgress(AltoClef mod, Task task, String reason, String stateKey, Object... fields) {
        logStateChange("VISIBLE_TASK_PROGRESS", mod, task, reason, stateKey, fields);
    }

    public static void logReturnTask(AltoClef mod, Task task, Task nextTask, String reason, String stateKey, Object... fields) {
        String nextTaskSummary = ChatClefDiagnostics.taskSummary(nextTask);
        Object[] merged = mergeFields(new Object[]{"nextTask", nextTaskSummary}, fields);
        logStateChange("VISIBLE_TASK_RETURN", mod, task, reason, stateKey + "|nextTask=" + nextTaskSummary, merged);
    }

    public static void logFinishedCheck(AltoClef mod, Task task, boolean finished, String reason, Object... fields) {
        logStateChange("VISIBLE_TASK_FINISHED_CHECK", mod, task, reason, Boolean.toString(finished), fields);
    }

    private static void logStateChange(String eventName,
                                       AltoClef mod,
                                       Task task,
                                       String reason,
                                       String stateKey,
                                       Object... fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        if (!markStateChanged(task, eventName + "|" + reason, stateKey)) {
            return;
        }
        boolean sourceEmissionCompleted = ChatClefDiagnostics.logBoundaryWithPhysicalOutcome(
                eventName,
                reason,
                task,
                mergeCommonFields(mod, fields)
        );
        if (sourceEmissionCompleted && "VISIBLE_TASK_RETURN".equals(eventName)) {
            CraftResourceRequirementSourceEventObserver.observeVisibleTaskReturn(task);
        }
    }

    private static boolean markStateChanged(Task task, String bucket, String stateKey) {
        if (task == null) {
            return true;
        }
        String normalizedStateKey = normalizeStateKey(stateKey);
        synchronized (LAST_STATE_BY_TASK) {
            Map<String, String> taskStates = LAST_STATE_BY_TASK.computeIfAbsent(task, ignored -> new HashMap<>());
            String previousStateKey = taskStates.get(bucket);
            if (normalizedStateKey.equals(previousStateKey)) {
                return false;
            }
            taskStates.put(bucket, normalizedStateKey);
            return true;
        }
    }

    private static Object[] mergeCommonFields(AltoClef mod, Object[] fields) {
        return mergeFields(new Object[]{
                "dimension", ChatClefDiagnostics.safeValue(WorldHelper::getCurrentDimension),
                "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getPathingBehavior().isPathing()),
                "customGoalActive", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getCustomGoalProcess().isActive()),
                "mainHandStack", ChatClefDiagnostics.safeValue(() -> mod == null || mod.getPlayer() == null ? null : mod.getPlayer().getMainHandStack()),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot)
        }, fields);
    }

    private static Object[] mergeFields(Object[] first, Object[] second) {
        if (first == null || first.length == 0) {
            return second == null ? new Object[0] : second;
        }
        if (second == null || second.length == 0) {
            return first;
        }
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    private static String normalizeStateKey(String stateKey) {
        String normalized = stateKey == null ? "none" : stateKey;
        if (normalized.length() <= MAX_STATE_KEY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_STATE_KEY_LENGTH) + "...";
    }
}
