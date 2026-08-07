package lavi.minecraft.diagnostics.container.store;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

import java.util.StringJoiner;

//20260807_kpopmodder: Keep StoreInAnyContainerTask diagnostic field assembly out of the upstream task.
final class StoreInAnyContainerDiagnosticFields {
    private static final int MAX_REPEAT_KEY_LENGTH = 280;

    private StoreInAnyContainerDiagnosticFields() {
    }

    static Object[] lifecycleFields(String trigger,
                                    Task task,
                                    boolean getIfNotPresent,
                                    ItemTarget[] toStore,
                                    Object[] extraFields) {
        return mergeFields(new Object[]{
                "diagnosticScope", "store_in_any_container",
                "owner", "store_in_any_container_observer",
                "mode", "BOUNDARY",
                "trigger", trigger,
                "dedupe_key", "store_in_any_container|" + trigger + "|" + ChatClefDiagnostics.className(task),
                "max_emission", "one_per_task_lifecycle_boundary",
                "correlation", "storeTaskIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", "stop".equals(trigger),
                "behavior_effect", "none",
                "storeTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task),
                "storeTaskIdentity", identity(task),
                "getIfNotPresent", getIfNotPresent,
                "toStore", ChatClefDiagnostics.itemTargets(toStore)
        }, extraFields);
    }

    static Object[] branchFields(String branch,
                                 String repeatKey,
                                 AltoClef mod,
                                 Task task,
                                 boolean getIfNotPresent,
                                 ItemTarget[] toStore,
                                 ItemTarget[] notStored,
                                 ContainerStoredTracker storedTracker,
                                 BlockPos closestContainer,
                                 boolean closestWithinRange,
                                 boolean currentTryWithinExtraRange,
                                 BlockPos currentChestTry,
                                 int dungeonChestCacheSize,
                                 int nonDungeonChestCacheSize,
                                 int suppressedRepeatCount,
                                 Object[] branchFields) {
        return mergeFields(new Object[]{
                "diagnosticScope", "store_in_any_container",
                "owner", "store_in_any_container_observer",
                "mode", "BOUNDARY",
                "trigger", branch,
                "dedupe_key", repeatKey,
                "max_emission", "detail_per_bucket=1,session="
                        + StoreInAnyContainerEmissionLimiter.SESSION_HARD_CAP
                        + ",summary_ticks="
                        + StoreInAnyContainerEmissionLimiter.REPEAT_SUMMARY_INTERVAL_TICKS,
                "correlation", "storeTaskIdentity=" + identity(task),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "suppressedRepeatCount", suppressedRepeatCount,
                "gameTick", ChatClefDiagnostics.currentClientTickId(),
                "dimension", ChatClefDiagnostics.safeValue(() -> mod == null || mod.getWorld() == null ? null : mod.getWorld().getRegistryKey().getValue()),
                "storeTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task),
                "storeTaskIdentity", identity(task),
                "getIfNotPresent", getIfNotPresent,
                "toStore", ChatClefDiagnostics.itemTargets(toStore),
                "notStored", ChatClefDiagnostics.itemTargets(notStored),
                "notStoredCount", notStored == null ? "unavailable" : notStored.length,
                "storedCountByTarget", storedCountByTarget(storedTracker, toStore),
                "closestContainerPresent", closestContainer != null,
                "closestContainerPosition", ChatClefDiagnostics.blockPos(closestContainer),
                "closestWithinRange", closestWithinRange,
                "currentTryWithinExtraRange", currentTryWithinExtraRange,
                "currentChestTry", ChatClefDiagnostics.blockPos(currentChestTry),
                "dungeonChestCacheSize", dungeonChestCacheSize,
                "nonDungeonChestCacheSize", nonDungeonChestCacheSize,
                "playerPosition", ChatClefDiagnostics.playerPosition(mod)
        }, branchFields);
    }

    static Object[] cap(String scope, int cap) {
        return new Object[]{
                "diagnosticScope", scope,
                "owner", "store_in_any_container_observer",
                "mode", "BOUNDARY",
                "trigger", "session_cap",
                "dedupe_key", scope + "|cap",
                "max_emission", "session=" + cap,
                "correlation", "session",
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "capScope", scope,
                "cap", cap
        };
    }

    static Object[] repeatSummary(String repeatKey, int suppressedRepeatCount) {
        return new Object[]{
                "diagnosticScope", "store_in_any_container",
                "owner", "store_in_any_container_observer",
                "mode", "BOUNDARY",
                "trigger", "repeat_summary",
                "dedupe_key", repeatKey,
                "max_emission", "summary_ticks=" + StoreInAnyContainerEmissionLimiter.REPEAT_SUMMARY_INTERVAL_TICKS,
                "correlation", "repeatKey=" + repeatKey,
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "repeatKey", repeatKey,
                "suppressedRepeatCount", suppressedRepeatCount
        };
    }

    static String repeatKey(String branch,
                            Task task,
                            ItemTarget[] notStored,
                            BlockPos closestContainer,
                            BlockPos currentChestTry) {
        String key = "STORE_IN_ANY_CONTAINER_BRANCH"
                + "|branch=" + branch
                + "|task=" + ChatClefDiagnostics.className(task)
                + "|notStoredCount=" + (notStored == null ? "unavailable" : notStored.length)
                + "|notStored=" + ChatClefDiagnostics.itemTargets(notStored)
                + "|closest=" + ChatClefDiagnostics.blockPos(closestContainer)
                + "|currentChestTry=" + ChatClefDiagnostics.blockPos(currentChestTry);
        return normalizeRepeatKey(key);
    }

    private static String storedCountByTarget(ContainerStoredTracker storedTracker, ItemTarget[] targets) {
        if (storedTracker == null || targets == null) {
            return "unavailable";
        }
        try {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            for (ItemTarget target : targets) {
                ItemTarget current = target;
                joiner.add(ChatClefDiagnostics.safeValueForDiagnosticLog(() ->
                        current + "=" + storedTracker.getStoredCount(current.getMatches()) + "/" + current.getTargetCount()
                ));
            }
            return joiner.toString();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable#error=" + error.getClass().getSimpleName();
        }
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

    private static String normalizeRepeatKey(String repeatKey) {
        if (repeatKey == null) {
            return "none";
        }
        if (repeatKey.length() <= MAX_REPEAT_KEY_LENGTH) {
            return repeatKey;
        }
        return repeatKey.substring(0, MAX_REPEAT_KEY_LENGTH) + "...";
    }

    private static String identity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }
}
