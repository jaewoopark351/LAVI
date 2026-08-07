package lavi.minecraft.diagnostics.container.store;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

//20260807_kpopmodder: Summarize StoreInAnyContainerTask progress signatures without changing storage behavior.
final class StoreInAnyContainerProgressDiagnostics {
    private static final long SUMMARY_INTERVAL_TICKS = 200;
    private static final int SESSION_HARD_CAP = 256;
    private static final int MAX_SIGNATURE_LENGTH = 360;

    private static final Map<String, ProgressState> STATES = new HashMap<>();
    private static int emittedCount;
    private static boolean capLogged;

    private StoreInAnyContainerProgressDiagnostics() {
    }

    static synchronized void observe(String branch,
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
                                     Object[] branchFields) {
        long tick = ChatClefDiagnostics.currentClientTickId();
        String taskIdentity = identity(task);
        String signature = progressSignature(branch, toStore, notStored, storedTracker, closestContainer,
                currentChestTry, branchFields);
        ProgressState state = STATES.get(taskIdentity);
        if (state == null) {
            state = new ProgressState(signature, tick);
            STATES.put(taskIdentity, state);
            state.totalObservations = 1;
            state.signatureObservationCount = 1;
            emit("first_observation", null, 0, 0, state, branch, mod, task, getIfNotPresent, toStore,
                    notStored, storedTracker, closestContainer, closestWithinRange, currentTryWithinExtraRange,
                    currentChestTry, dungeonChestCacheSize, nonDungeonChestCacheSize, branchFields);
            return;
        }

        state.totalObservations++;
        if (!signature.equals(state.signature)) {
            String previousSignature = state.signature;
            long previousStableTicks = tick - state.signatureStartTick;
            int previousObservationCount = state.signatureObservationCount;
            state.signature = signature;
            state.signatureStartTick = tick;
            state.signatureObservationCount = 1;
            state.branchChangeCount++;
            emit("progress_signature_changed", previousSignature, previousStableTicks, previousObservationCount,
                    state, branch, mod, task, getIfNotPresent, toStore, notStored, storedTracker, closestContainer,
                    closestWithinRange, currentTryWithinExtraRange, currentChestTry, dungeonChestCacheSize,
                    nonDungeonChestCacheSize, branchFields);
            return;
        }

        state.signatureObservationCount++;
        if (tick - state.lastEmissionTick < SUMMARY_INTERVAL_TICKS) {
            return;
        }
        emit("progress_stable_summary", null, 0,
                0, state, branch, mod, task, getIfNotPresent, toStore, notStored,
                storedTracker, closestContainer, closestWithinRange, currentTryWithinExtraRange, currentChestTry,
                dungeonChestCacheSize, nonDungeonChestCacheSize, branchFields);
    }

    private static void emit(String trigger,
                             String previousSignature,
                             long previousStableTicks,
                             int previousObservationCount,
                             ProgressState state,
                             String branch,
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
                             Object[] branchFields) {
        if (emittedCount >= SESSION_HARD_CAP) {
            if (!capLogged) {
                capLogged = true;
                ChatClefDiagnostics.logBoundary("STORE_IN_ANY_CONTAINER_PROGRESS_DIAGNOSTIC_CAP_REACHED",
                        "store_in_any_container_progress_diagnostic_cap_reached",
                        task,
                        ChatClefDiagnostics.withCommandContextFields(
                                "diagnosticScope", "store_in_any_container_progress",
                                "owner", "store_in_any_container_progress_observer",
                                "mode", "BOUNDARY",
                                "trigger", "session_cap",
                                "dedupe_key", "store_in_any_container_progress|cap",
                                "max_emission", "session=" + SESSION_HARD_CAP,
                                "correlation", "session",
                                "payload", "flat_fields",
                                "terminal", false,
                                "behavior_effect", "none",
                                "cap", SESSION_HARD_CAP
                        ));
            }
            return;
        }

        long tick = ChatClefDiagnostics.currentClientTickId();
        emittedCount++;
        state.lastEmissionTick = tick;
        ChatClefDiagnostics.logBoundary("STORE_IN_ANY_CONTAINER_PROGRESS_STATE",
                "store_in_any_container_progress_state",
                task,
                ChatClefDiagnostics.withCommandContextFields(
                        "diagnosticScope", "store_in_any_container_progress",
                        "owner", "store_in_any_container_progress_observer",
                        "mode", "BOUNDARY",
                        "trigger", trigger,
                        "dedupe_key", "store_progress|" + identity(task) + "|" + state.signature,
                        "max_emission", "first_change_summary_ticks=" + SUMMARY_INTERVAL_TICKS + ",session=" + SESSION_HARD_CAP,
                        "correlation", "storeTaskIdentity=" + identity(task),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "gameTick", tick,
                        "storeTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task),
                        "storeTaskIdentity", identity(task),
                        "branch", branch,
                        "progressSignature", state.signature,
                        "previousProgressSignature", previousSignature == null ? "none" : previousSignature,
                        "stableSignatureTicks", tick - state.signatureStartTick,
                        "previousStableSignatureTicks", previousStableTicks,
                        "sameSignatureObservationCount", state.signatureObservationCount,
                        "previousSignatureObservationCount", previousObservationCount,
                        "totalObservationCount", state.totalObservations,
                        "branchChangeCount", state.branchChangeCount,
                        "noProgressTicks", tick - state.signatureStartTick,
                        "getIfNotPresent", getIfNotPresent,
                        "toStore", ChatClefDiagnostics.itemTargets(toStore),
                        "notStored", ChatClefDiagnostics.itemTargets(notStored),
                        "notStoredCount", notStored == null ? "unavailable" : notStored.length,
                        "storedCountByTarget", storedCountByTarget(storedTracker, toStore),
                        "currentChildTask", diagnosticValue(field(branchFields, "childTaskClass")),
                        "closestContainerPresent", closestContainer != null,
                        "closestContainerPosition", ChatClefDiagnostics.blockPos(closestContainer),
                        "closestWithinRange", closestWithinRange,
                        "currentTryWithinExtraRange", currentTryWithinExtraRange,
                        "currentChestTry", ChatClefDiagnostics.blockPos(currentChestTry),
                        "dungeonChestCacheSize", dungeonChestCacheSize,
                        "nonDungeonChestCacheSize", nonDungeonChestCacheSize,
                        "requestedItem", diagnosticValue(field(branchFields, "requestedItem")),
                        "containerBlockItem", diagnosticValue(field(branchFields, "containerBlockItem")),
                        "missingTarget", diagnosticValue(field(branchFields, "missingTarget")),
                        "progressCheckOk", diagnosticValue(field(branchFields, "progressCheckOk")),
                        "playerPosition", ChatClefDiagnostics.playerPosition(mod)
                ));
    }

    private static String progressSignature(String branch,
                                            ItemTarget[] toStore,
                                            ItemTarget[] notStored,
                                            ContainerStoredTracker storedTracker,
                                            BlockPos closestContainer,
                                            BlockPos currentChestTry,
                                            Object[] branchFields) {
        String key = "branch=" + branch
                + "|toStore=" + ChatClefDiagnostics.itemTargets(toStore)
                + "|notStored=" + ChatClefDiagnostics.itemTargets(notStored)
                + "|stored=" + storedCountByTarget(storedTracker, toStore)
                + "|child=" + diagnosticValue(field(branchFields, "childTaskClass"))
                + "|closest=" + ChatClefDiagnostics.blockPos(closestContainer)
                + "|currentChestTry=" + ChatClefDiagnostics.blockPos(currentChestTry)
                + "|requestedItem=" + diagnosticValue(field(branchFields, "requestedItem"))
                + "|containerBlockItem=" + diagnosticValue(field(branchFields, "containerBlockItem"))
                + "|missingTarget=" + diagnosticValue(field(branchFields, "missingTarget"));
        if (key.length() <= MAX_SIGNATURE_LENGTH) {
            return key;
        }
        return key.substring(0, MAX_SIGNATURE_LENGTH) + "...";
    }

    private static Object field(Object[] fields, String name) {
        if (fields == null || name == null) {
            return null;
        }
        for (int i = 0; i + 1 < fields.length; i += 2) {
            Object key = fields[i];
            if (name.equals(key)) {
                return fields[i + 1];
            }
        }
        return null;
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

    private static String diagnosticValue(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
    }

    private static String identity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }

    private static final class ProgressState {
        private String signature;
        private long signatureStartTick;
        private long lastEmissionTick;
        private int signatureObservationCount;
        private int totalObservations;
        private int branchChangeCount;

        private ProgressState(String signature, long tick) {
            this.signature = signature;
            this.signatureStartTick = tick;
            this.lastEmissionTick = tick;
        }
    }
}
